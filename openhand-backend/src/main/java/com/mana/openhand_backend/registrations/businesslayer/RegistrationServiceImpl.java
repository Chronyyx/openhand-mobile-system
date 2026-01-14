package com.mana.openhand_backend.registrations.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.events.utils.EventTitleResolver;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.utils.AlreadyRegisteredException;
import com.mana.openhand_backend.registrations.utils.EventCapacityException;
import com.mana.openhand_backend.registrations.utils.RegistrationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SendGridEmailService sendGridEmailService;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            SendGridEmailService sendGridEmailService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.sendGridEmailService = sendGridEmailService;
    }

    /**
     * Registers a user for an event with atomic capacity checking to prevent race conditions.
     *
     * Uses pessimistic locking at the database level to ensure that capacity checks and
     * registration updates happen atomically. This prevents multiple concurrent registrations
     * from exceeding event capacity limits.
     *
     * Flow:
     * 1. Verify user exists
     * 2. Lock the event row (pessimistic write lock) to prevent concurrent capacity violations
     * 3. Atomically check if event has capacity
     * 4. If capacity available: create CONFIRMED registration and increment counter
     * 5. If at capacity: create WAITLISTED registration
     * 6. Update event status based on new capacity
     *
     * @param userId the user attempting to register
     * @param eventId the event to register for
     * @return the created or reactivated registration
     * @throws EventCapacityException if event reaches capacity during a race condition
     * @throws AlreadyRegisteredException if user already has an active registration
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Registration registerForEvent(Long userId, Long eventId) {
        // Verify user exists
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Check for existing registration BEFORE locking (quick check)
        Optional<Registration> existingRegistrationOpt = registrationRepository.findByUserIdAndEventId(userId, eventId);
        Registration registration;

        if (existingRegistrationOpt.isPresent()) {
            Registration existing = existingRegistrationOpt.get();
            if (existing.getStatus() != RegistrationStatus.CANCELLED) {
                throw new AlreadyRegisteredException(userId, eventId);
            }
            // Reactivate cancelled registration
            registration = existing;
            registration.setCancelledAt(null);
            registration.setConfirmedAt(null);
            registration.setWaitlistedPosition(null);
            registration.setRequestedAt(LocalDateTime.now());
        } else {
            // Create new registration entity
            registration = new Registration(user, null);
            registration.setRequestedAt(LocalDateTime.now());
        }

        /**
         * CRITICAL SECTION: Pessimistic lock the event to ensure atomic capacity checking.
         * This prevents the following race condition:
         *
         * Thread A: Check capacity (capacity = 10, current = 9) → can register
         * Thread B: Check capacity (capacity = 10, current = 9) → can register
         * Thread A: Register (increment to 10)
         * Thread B: Register (increment to 11) ← OVERBOOKING!
         *
         * With pessimistic lock, Thread B waits for Thread A's transaction to complete,
         * then sees current = 10 and triggers waitlist instead.
         */
        Event lockedEvent = registrationRepository.findEventByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Now we hold an exclusive lock on the event row. Safe to check and update.
        Integer currentRegs = lockedEvent.getCurrentRegistrations();
        if (currentRegs == null) {
            currentRegs = 0;
        }

        // Atomic capacity check
        boolean atCapacity = false;
        if (lockedEvent.getMaxCapacity() != null && currentRegs >= lockedEvent.getMaxCapacity()) {
            atCapacity = true;
        }

        if (atCapacity) {
            // Event is at capacity - add to waitlist
            long waitlistCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
            registration.setStatus(RegistrationStatus.WAITLISTED);
            registration.setWaitlistedPosition((int) (waitlistCount + 1));
            registration.setEvent(lockedEvent);
            // Note: NOT throwing EventCapacityException here. User is placed on waitlist instead.
        } else {
            // Space available - confirm immediately
            registration.setStatus(RegistrationStatus.CONFIRMED);
            registration.setConfirmedAt(LocalDateTime.now());
            registration.setEvent(lockedEvent);

            // Atomically increment event's current registrations count
            lockedEvent.setCurrentRegistrations(currentRegs + 1);

            // Update event status based on new capacity
            if (lockedEvent.getMaxCapacity() != null) {
                if (lockedEvent.getCurrentRegistrations() >= lockedEvent.getMaxCapacity()) {
                    lockedEvent.setStatus(EventStatus.FULL);
                } else if (lockedEvent.getCurrentRegistrations() >= lockedEvent.getMaxCapacity() * 0.8) {
                    lockedEvent.setStatus(EventStatus.NEARLY_FULL);
                }
            }

            // Save the updated event (lock is still held until transaction commits)
            eventRepository.save(lockedEvent);
        }

        // Save registration and return (event lock is released after transaction commits)
        Registration savedRegistration = registrationRepository.save(registration);

        // Create notification for successful registration confirmation
        try {
            String userLanguage = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";
            notificationService.createNotification(
                    userId,
                    eventId,
                    "REGISTRATION_CONFIRMATION",
                    userLanguage
            );
        } catch (Exception e) {
            // Log but don't throw - notification creation failure shouldn't block registration
            System.err.println("Failed to create registration confirmation notification: " + e.getMessage());
        }

        // Fire-and-forget external email
        if (savedRegistration.getStatus() == RegistrationStatus.CONFIRMED) {
            sendRegistrationConfirmationEmail(user, lockedEvent);
        }

        return savedRegistration;
    }

    @Override
    public Registration getRegistrationById(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new RegistrationNotFoundException(id));
    }

    @Override
    public List<Registration> getUserRegistrations(Long userId) {
        return registrationRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public Registration cancelRegistration(Long userId, Long eventId) {
        @SuppressWarnings("null")
        Registration registration = registrationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new RuntimeException(
                        "Registration not found for user " + userId + " and event " + eventId));

        // If this was a confirmed registration, update event capacity
        if (registration.getStatus() == RegistrationStatus.CONFIRMED) {
            Event event = registration.getEvent();
            if (event.getCurrentRegistrations() != null && event.getCurrentRegistrations() > 0) {
                event.setCurrentRegistrations(event.getCurrentRegistrations() - 1);

                // Update event status
                if (event.getMaxCapacity() != null) {
                    if (event.getCurrentRegistrations() < event.getMaxCapacity()) {
                        if (event.getCurrentRegistrations() >= event.getMaxCapacity() * 0.8) {
                            event.setStatus(EventStatus.NEARLY_FULL);
                        } else {
                            event.setStatus(EventStatus.OPEN);
                        }
                    }
                }

                eventRepository.save(event);
            }
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());

        Registration cancelledRegistration = registrationRepository.save(registration);

        // Create cancellation notification
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            String userLanguage = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";
            notificationService.createNotification(
                    userId,
                    eventId,
                    "CANCELLATION",
                    userLanguage
            );
        } catch (Exception e) {
            // Log but don't throw - notification creation failure shouldn't block cancellation
            System.err.println("Failed to create cancellation notification: " + e.getMessage());
        }

        sendCancellationEmail(registration.getUser(), registration.getEvent(), "Registration cancelled by member.");

        return cancelledRegistration;
    }

    private void sendRegistrationConfirmationEmail(User user, Event event) {
        try {
            String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";
            String resolvedEventTitle = EventTitleResolver.resolve(event.getTitle(), language);
            sendGridEmailService.sendRegistrationConfirmation(
                    user.getEmail(),
                    user.getName(),
                    resolvedEventTitle,
                    language
            );
        } catch (Exception ex) {
            logger.error("Failed to send registration confirmation email for user {} and event {}: {}",
                    user.getId(), event.getId(), ex.getMessage());
        }
    }

    private void sendCancellationEmail(User user, Event event, String details) {
        try {
            String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";
            String resolvedEventTitle = EventTitleResolver.resolve(event.getTitle(), language);
            sendGridEmailService.sendCancellationOrUpdate(
                    user.getEmail(),
                    user.getName(),
                    resolvedEventTitle,
                    details,
                    language
            );
        } catch (Exception ex) {
            logger.error("Failed to send cancellation/update email for user {} and event {}: {}",
                    user.getId(), event.getId(), ex.getMessage());
        }
    }
}

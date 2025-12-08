package com.mana.openhand_backend.registrations.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.utils.AlreadyRegisteredException;
import com.mana.openhand_backend.registrations.utils.RegistrationNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository,
                                   EventRepository eventRepository,
                                   UserRepository userRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Registration registerForEvent(Long userId, Long eventId) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Verify event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Check if already registered
        if (registrationRepository.existsByUserIdAndEventId(userId, eventId)) {
            throw new AlreadyRegisteredException(userId, eventId);
        }

        // Determine registration status based on event capacity (using multiple signals)
        long confirmedCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        Integer currentRegs = event.getCurrentRegistrations();
        boolean atCapacity = false;

        if (event.getMaxCapacity() != null) {
            atCapacity = confirmedCount >= event.getMaxCapacity()
                    || (currentRegs != null && currentRegs >= event.getMaxCapacity())
                    || event.getStatus() == EventStatus.FULL;
        }

        Registration registration = new Registration(user, event);

        if (atCapacity) {
            // Event is at capacity - add to waitlist
            long waitlistCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
            registration.setStatus(RegistrationStatus.WAITLISTED);
            registration.setWaitlistedPosition((int) (waitlistCount + 1));
        } else {
            // Space available - confirm immediately
            registration.setStatus(RegistrationStatus.CONFIRMED);
            registration.setConfirmedAt(LocalDateTime.now());

            // Update event's current registrations count
            event.setCurrentRegistrations(event.getCurrentRegistrations() != null
                    ? event.getCurrentRegistrations() + 1
                    : 1);

            // Update event status if needed
            if (event.getMaxCapacity() != null) {
                if (event.getCurrentRegistrations() >= event.getMaxCapacity()) {
                    event.setStatus(EventStatus.FULL);
                } else if (event.getCurrentRegistrations() >= event.getMaxCapacity() * 0.8) {
                    event.setStatus(EventStatus.NEARLY_FULL);
                }
            }

            eventRepository.save(event);
        }

        return registrationRepository.save(registration);
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
        Registration registration = registrationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new RuntimeException("Registration not found for user " + userId + " and event " + eventId));

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());

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

        return registrationRepository.save(registration);
    }
}

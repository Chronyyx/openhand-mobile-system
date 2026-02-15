package com.mana.openhand_backend.registrations.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.businesslayer.EventCompletionService;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.events.utils.EventTitleResolver;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.FamilyMemberRequestModel;
import com.mana.openhand_backend.registrations.domainclientlayer.GroupRegistrationResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryFilter;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationTimeCategory;
import com.mana.openhand_backend.registrations.utils.AlreadyRegisteredException;
import com.mana.openhand_backend.registrations.utils.EventCapacityException;
import com.mana.openhand_backend.registrations.utils.EventCompletedException;
import com.mana.openhand_backend.registrations.utils.GroupRegistrationCapacityException;
import com.mana.openhand_backend.registrations.utils.RegistrationNotFoundException;
import com.mana.openhand_backend.registrations.utils.RegistrationHistoryResponseMapper;
import com.mana.openhand_backend.registrations.utils.GroupRegistrationResponseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository notificationRepository;
    private final SendGridEmailService sendGridEmailService;
    private final EventCompletionService eventCompletionService;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository notificationRepository,
            SendGridEmailService sendGridEmailService,
            EventCompletionService eventCompletionService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.sendGridEmailService = sendGridEmailService;
        this.eventCompletionService = eventCompletionService;
    }

    /**
     * Registers a user for an event with atomic capacity checking to prevent race
     * conditions.
     *
     * Uses pessimistic locking at the database level to ensure that capacity checks
     * and
     * registration updates happen atomically. This prevents multiple concurrent
     * registrations
     * from exceeding event capacity limits.
     *
     * Flow:
     * 1. Verify user exists
     * 2. Lock the event row (pessimistic write lock) to prevent concurrent capacity
     * violations
     * 3. Atomically check if event has capacity
     * 4. If capacity available: create CONFIRMED registration and increment counter
     * 5. If at capacity: create WAITLISTED registration
     * 6. Update event status based on new capacity
     *
     * @param userId  the user attempting to register
     * @param eventId the event to register for
     * @return the created or reactivated registration
     * @throws EventCapacityException     if event reaches capacity during a race
     *                                    condition
     * @throws AlreadyRegisteredException if user already has an active registration
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Registration registerForEvent(Long userId, Long eventId) {
        return registerSingleParticipant(userId, eventId, true);
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public GroupRegistrationResponseModel registerForEventWithFamily(Long userId, Long eventId,
            List<FamilyMemberRequestModel> familyMembers) {
        List<FamilyMemberRequestModel> safeFamilyMembers = familyMembers == null ? List.of() : familyMembers;

        if (safeFamilyMembers.isEmpty()) {
            Registration registration = registerSingleParticipant(userId, eventId, true);
            Event event = registration.getEvent();
            return GroupRegistrationResponseMapper.toResponse(event, List.of(registration));
        }

        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getMemberStatus() != null && user.getMemberStatus().name().equals("INACTIVE")) {
            throw new com.mana.openhand_backend.registrations.utils.InactiveMemberException(userId);
        }

        Optional<Registration> existingRegistrationOpt = registrationRepository.findByUserIdAndEventId(userId, eventId);
        Registration primaryRegistration;

        if (existingRegistrationOpt.isPresent()) {
            Registration existing = existingRegistrationOpt.get();
            if (existing.getStatus() != RegistrationStatus.CANCELLED) {
                throw new AlreadyRegisteredException(userId, eventId);
            }
            primaryRegistration = existing;
            primaryRegistration.setCancelledAt(null);
            primaryRegistration.setConfirmedAt(null);
            primaryRegistration.setWaitlistedPosition(null);
            primaryRegistration.setRequestedAt(LocalDateTime.now());
        } else {
            primaryRegistration = new Registration(user, null);
            primaryRegistration.setRequestedAt(LocalDateTime.now());
        }

        Event lockedEvent = registrationRepository.findEventByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        LocalDateTime now = LocalDateTime.now();
        if (eventCompletionService.ensureCompletedIfEnded(lockedEvent, now)) {
            throw new EventCompletedException(eventId);
        }

        int currentRegs = lockedEvent.getCurrentRegistrations() != null ? lockedEvent.getCurrentRegistrations() : 0;
        int totalParticipants = 1 + safeFamilyMembers.size();

        if (lockedEvent.getMaxCapacity() != null) {
            int remainingCapacity = lockedEvent.getMaxCapacity() - currentRegs;
            if (remainingCapacity < totalParticipants) {
                throw new GroupRegistrationCapacityException(eventId, totalParticipants,
                        Math.max(0, remainingCapacity));
            }
        }

        String groupId = UUID.randomUUID().toString();

        primaryRegistration.setStatus(RegistrationStatus.CONFIRMED);
        primaryRegistration.setConfirmedAt(LocalDateTime.now());
        primaryRegistration.setEvent(lockedEvent);
        primaryRegistration.setRegistrationGroupId(groupId);
        primaryRegistration.setPrimaryRegistrant(true);
        primaryRegistration.setPrimaryUserId(userId);

        List<Registration> familyRegistrations = safeFamilyMembers.stream()
                .map(member -> buildFamilyRegistration(member, lockedEvent, groupId, userId))
                .collect(Collectors.toList());

        lockedEvent.setCurrentRegistrations(currentRegs + totalParticipants);
        updateEventStatusForCapacity(lockedEvent);
        eventRepository.save(lockedEvent);

        Registration savedPrimary = registrationRepository.save(primaryRegistration);
        List<Registration> savedFamily = familyRegistrations.isEmpty()
                ? List.of()
                : registrationRepository.saveAll(familyRegistrations);

        if (savedPrimary.getStatus() == RegistrationStatus.CONFIRMED) {
            sendRegistrationConfirmationEmail(user, lockedEvent, buildParticipantNames(savedPrimary, savedFamily));

            String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";
            notificationService.createNotification(
                    user.getId(),
                    eventId,
                    "REGISTRATION_CONFIRMATION",
                    language);
        }

        List<Registration> allRegistrations = new java.util.ArrayList<>();
        allRegistrations.add(savedPrimary);
        allRegistrations.addAll(savedFamily);

        return GroupRegistrationResponseMapper.toResponse(lockedEvent, allRegistrations);
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
    public List<RegistrationHistoryResponseModel> getUserRegistrationHistory(
            Long userId,
            RegistrationHistoryFilter filter) {
        List<Registration> registrations = registrationRepository.findByUserIdWithEvent(userId);
        LocalDateTime now = LocalDateTime.now();

        List<RegistrationWithCategory> categorized = registrations.stream()
                .map(registration -> new RegistrationWithCategory(
                        registration,
                        resolveTimeCategory(registration, now)))
                .filter(item -> filter == RegistrationHistoryFilter.ALL
                        || item.timeCategory == mapFilterToCategory(filter))
                .sorted(registrationHistoryComparator())
                .collect(Collectors.toList());

        return categorized.stream()
                .map(item -> {
                    List<Registration> groupRegistrations = resolveGroupRegistrations(item.registration);
                    return RegistrationHistoryResponseMapper.toResponseModel(
                            item.registration,
                            item.timeCategory,
                            GroupRegistrationResponseMapper.toParticipants(groupRegistrations));
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Registration cancelRegistration(Long userId, Long eventId) {
        @SuppressWarnings("null")
        Registration registration = registrationRepository.findByUserIdAndEventId(userId, eventId)
                .orElseThrow(() -> new RuntimeException(
                        "Registration not found for user " + userId + " and event " + eventId));

        return cancelRegistrationInternal(registration, "Registration cancelled by member.", false, eventId);
    }

    @Override
    @Transactional
    public void cancelRegistrationsForUser(Long userId, String reason) {
        List<Registration> registrations = registrationRepository.findByUserId(userId);
        if (registrations.isEmpty()) {
            return;
        }

        for (Registration registration : registrations) {
            if (registration.getStatus() == RegistrationStatus.CANCELLED) {
                continue;
            }
            try {
                Long eventId = registration.getEvent() != null ? registration.getEvent().getId() : null;
                cancelRegistrationInternal(registration, reason, true, eventId);
            } catch (RuntimeException ex) {
                logger.warn("Failed to cancel registration {} for deactivated user {}: {}",
                        registration.getId(), userId, ex.getMessage());
            }
        }
    }

    private Registration cancelRegistrationInternal(Registration registration, String reason,
            boolean skipCompletionCheck,
            Long eventIdOverride) {
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            return registration;
        }

        Event event = registration.getEvent();
        if (!skipCompletionCheck && event != null
                && eventCompletionService.ensureCompletedIfEnded(event, LocalDateTime.now())) {
            throw new EventCompletedException(event.getId() != null ? event.getId() : eventIdOverride);
        }

        Registration cancelledRegistration;
        String groupId = registration.getRegistrationGroupId();
        Long eventId = eventIdOverride != null ? eventIdOverride : (event != null ? event.getId() : null);

        if (groupId != null) {
            List<Registration> groupRegistrations = eventId != null
                    ? registrationRepository.findByEventIdAndRegistrationGroupId(eventId, groupId)
                    : registrationRepository.findByRegistrationGroupId(groupId);

            if (groupRegistrations.isEmpty()) {
                groupRegistrations = List.of(registration);
            }

            // Filter to only registrations that aren't already cancelled
            List<Registration> toCancel = groupRegistrations.stream()
                    .filter(reg -> reg.getStatus() != RegistrationStatus.CANCELLED)
                    .collect(Collectors.toList());

            // If nothing to cancel, just return the registration
            if (toCancel.isEmpty()) {
                return registration;
            }

            int confirmedCount = (int) toCancel.stream()
                    .filter(reg -> reg.getStatus() == RegistrationStatus.CONFIRMED)
                    .count();

            if (event != null && event.getCurrentRegistrations() != null && event.getCurrentRegistrations() > 0) {
                int updatedCount = Math.max(0, event.getCurrentRegistrations() - confirmedCount);
                event.setCurrentRegistrations(updatedCount);
                updateEventStatusForCapacity(event);
                eventRepository.save(event);
            }

            LocalDateTime cancelledAt = LocalDateTime.now();
            toCancel.forEach(reg -> {
                reg.setStatus(RegistrationStatus.CANCELLED);
                reg.setCancelledAt(cancelledAt);
            });

            List<Registration> cancelledGroup = registrationRepository.saveAll(toCancel);
            cancelledRegistration = cancelledGroup.stream()
                    .filter(reg -> Objects.equals(reg.getId(), registration.getId()))
                    .findFirst()
                    .orElse(registration);
        } else {
            if (registration.getStatus() == RegistrationStatus.CONFIRMED) {
                if (event != null && event.getCurrentRegistrations() != null && event.getCurrentRegistrations() > 0) {
                    event.setCurrentRegistrations(event.getCurrentRegistrations() - 1);
                    updateEventStatusForCapacity(event);
                    eventRepository.save(event);
                }
            }

            registration.setStatus(RegistrationStatus.CANCELLED);
            registration.setCancelledAt(LocalDateTime.now());
            cancelledRegistration = registrationRepository.save(registration);
        }

        sendCancellationEmail(registration.getUser(), event, reason);

        return cancelledRegistration;
    }

    private RegistrationTimeCategory resolveTimeCategory(Registration registration, LocalDateTime now) {
        if (registration.getEvent() == null) {
            return RegistrationTimeCategory.PAST;
        }

        Event event = registration.getEvent();
        if (event.getStatus() == EventStatus.COMPLETED) {
            return RegistrationTimeCategory.PAST;
        }

        if (event.getEndDateTime() != null && !event.getEndDateTime().isAfter(now)) {
            return RegistrationTimeCategory.PAST;
        }

        if (event.getStartDateTime() == null) {
            return RegistrationTimeCategory.PAST;
        }

        return event.getStartDateTime().isAfter(now)
                ? RegistrationTimeCategory.ACTIVE
                : RegistrationTimeCategory.PAST;
    }

    private RegistrationTimeCategory mapFilterToCategory(RegistrationHistoryFilter filter) {
        if (filter == RegistrationHistoryFilter.ACTIVE) {
            return RegistrationTimeCategory.ACTIVE;
        }
        return RegistrationTimeCategory.PAST;
    }

    private Comparator<RegistrationWithCategory> registrationHistoryComparator() {
        return (left, right) -> {
            int leftGroup = left.timeCategory == RegistrationTimeCategory.ACTIVE ? 0 : 1;
            int rightGroup = right.timeCategory == RegistrationTimeCategory.ACTIVE ? 0 : 1;

            if (leftGroup != rightGroup) {
                return Integer.compare(leftGroup, rightGroup);
            }

            LocalDateTime leftStart = left.registration.getEvent().getStartDateTime();
            LocalDateTime rightStart = right.registration.getEvent().getStartDateTime();

            if (left.timeCategory == RegistrationTimeCategory.ACTIVE) {
                return leftStart.compareTo(rightStart);
            }

            return rightStart.compareTo(leftStart);
        };
    }

    private static class RegistrationWithCategory {
        private final Registration registration;
        private final RegistrationTimeCategory timeCategory;

        private RegistrationWithCategory(Registration registration, RegistrationTimeCategory timeCategory) {
            this.registration = registration;
            this.timeCategory = timeCategory;
        }
    }

    private List<Registration> resolveGroupRegistrations(Registration registration) {
        String groupId = registration.getRegistrationGroupId();
        if (groupId == null || groupId.isBlank()) {
            return List.of(registration);
        }
        List<Registration> groupRegistrations = registrationRepository.findByRegistrationGroupId(groupId);
        return groupRegistrations.isEmpty() ? List.of(registration) : groupRegistrations;
    }

    private Registration registerSingleParticipant(Long userId, Long eventId, boolean allowWaitlist) {
        @SuppressWarnings("null")
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getMemberStatus() != null && user.getMemberStatus().name().equals("INACTIVE")) {
            throw new com.mana.openhand_backend.registrations.utils.InactiveMemberException(userId);
        }

        Optional<Registration> existingRegistrationOpt = registrationRepository.findByUserIdAndEventId(userId, eventId);
        Registration registration;

        if (existingRegistrationOpt.isPresent()) {
            Registration existing = existingRegistrationOpt.get();
            if (existing.getStatus() != RegistrationStatus.CANCELLED) {
                throw new AlreadyRegisteredException(userId, eventId);
            }
            registration = existing;
            registration.setCancelledAt(null);
            registration.setConfirmedAt(null);
            registration.setWaitlistedPosition(null);
            registration.setRequestedAt(LocalDateTime.now());
        } else {
            registration = new Registration(user, null);
            registration.setRequestedAt(LocalDateTime.now());
        }

        Event lockedEvent = registrationRepository.findEventByIdForUpdate(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        LocalDateTime now = LocalDateTime.now();
        if (eventCompletionService.ensureCompletedIfEnded(lockedEvent, now)) {
            throw new EventCompletedException(eventId);
        }

        int currentRegs = lockedEvent.getCurrentRegistrations() != null ? lockedEvent.getCurrentRegistrations() : 0;
        boolean atCapacity = false;
        if (lockedEvent.getMaxCapacity() != null && currentRegs >= lockedEvent.getMaxCapacity()) {
            atCapacity = true;
        }

        if (atCapacity && allowWaitlist) {
            long waitlistCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
            registration.setStatus(RegistrationStatus.WAITLISTED);
            registration.setWaitlistedPosition((int) (waitlistCount + 1));
            registration.setEvent(lockedEvent);
        } else if (atCapacity) {
            throw new EventCapacityException(eventId);
        } else {
            registration.setStatus(RegistrationStatus.CONFIRMED);
            registration.setConfirmedAt(LocalDateTime.now());
            registration.setEvent(lockedEvent);

            lockedEvent.setCurrentRegistrations(currentRegs + 1);
            updateEventStatusForCapacity(lockedEvent);
            eventRepository.save(lockedEvent);
        }

        Registration savedRegistration = registrationRepository.save(registration);

        if (savedRegistration.getStatus() == RegistrationStatus.CONFIRMED) {
            sendRegistrationConfirmationEmail(user, lockedEvent, List.of(resolveParticipantName(savedRegistration)));

            String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";
            notificationService.createNotification(
                    user.getId(),
                    eventId,
                    "REGISTRATION_CONFIRMATION",
                    language);
        }

        return savedRegistration;
    }

    private Registration buildFamilyRegistration(FamilyMemberRequestModel member, Event event, String groupId,
            Long primaryUserId) {
        Registration registration = new Registration(null, event);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setConfirmedAt(LocalDateTime.now());
        registration.setRegistrationGroupId(groupId);
        registration.setPrimaryRegistrant(false);
        registration.setPrimaryUserId(primaryUserId);

        String fullName = member.getFullName() != null ? member.getFullName().trim() : null;
        registration.setParticipantFullName(fullName);
        registration.setParticipantAge(member.getAge());
        registration.setParticipantRelation(member.getRelation());

        if (member.getDateOfBirth() != null && !member.getDateOfBirth().isBlank()) {
            registration.setParticipantDateOfBirth(LocalDate.parse(member.getDateOfBirth()));
        }

        return registration;
    }

    private void updateEventStatusForCapacity(Event event) {
        if (event.getMaxCapacity() == null) {
            return;
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            return;
        }

        EventStatus oldStatus = event.getStatus();
        EventStatus newStatus = EventStatus.OPEN;

        int current = event.getCurrentRegistrations() != null ? event.getCurrentRegistrations() : 0;
        if (current >= event.getMaxCapacity()) {
            newStatus = EventStatus.FULL;
        } else if (current >= event.getMaxCapacity() * 0.8) {
            newStatus = EventStatus.NEARLY_FULL;
        }

        // Only update and trigger if status has CHANGED
        if (oldStatus != newStatus) {
            event.setStatus(newStatus);

            // Trigger notifications based on the NEW status
            if (newStatus == EventStatus.FULL) {
                checkAndTriggerCapacityNotifications(event, true, false);
            } else if (newStatus == EventStatus.NEARLY_FULL) {
                checkAndTriggerCapacityNotifications(event, false, true);
            }
        }
    }

    private void checkAndTriggerCapacityNotifications(Event event, boolean isFull, boolean isNearlyFull) {
        try {
            com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType type = null;
            if (isFull) {
                type = com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType.EVENT_FULL_ALERT;
            } else if (isNearlyFull) {
                type = com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType.EVENT_CAPACITY_WARNING;
            }

            if (type == null)
                return;

            // Find all employees
            List<User> employees = userRepository
                    .findByRolesContaining(com.mana.openhand_backend.identity.utils.RoleUtils.ROLE_EMPLOYEE);

            for (User employee : employees) {
                String language = employee.getPreferredLanguage() != null ? employee.getPreferredLanguage() : "en";
                notificationService.createNotification(
                        employee.getId(),
                        event.getId(),
                        type.name(),
                        language);
            }

        } catch (Exception e) {
            logger.error("Failed to process capacity notifications for event {}: {}", event.getId(), e.getMessage());
        }
    }

    private String resolveParticipantName(Registration registration) {
        if (registration.getUser() != null && registration.getUser().getName() != null) {
            return registration.getUser().getName();
        }
        return registration.getParticipantFullName() != null ? registration.getParticipantFullName() : "Participant";
    }

    private List<String> buildParticipantNames(Registration primary, List<Registration> family) {
        List<String> names = new java.util.ArrayList<>();
        names.add(resolveParticipantName(primary));
        for (Registration registration : family) {
            names.add(resolveParticipantName(registration));
        }
        return names;
    }

    private void sendRegistrationConfirmationEmail(User user, Event event, List<String> participantNames) {
        try {
            String language = user.getPreferredLanguage() != null ? user.getPreferredLanguage() : "en";
            String resolvedEventTitle = EventTitleResolver.resolve(event.getTitle(), language);
            sendGridEmailService.sendRegistrationConfirmation(
                    user.getEmail(),
                    user.getName(),
                    resolvedEventTitle,
                    language,
                    participantNames);
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
                    language);
        } catch (Exception ex) {
            logger.error("Failed to send cancellation/update email for user {} and event {}: {}",
                    user.getId(), event.getId(), ex.getMessage());
        }
    }
}

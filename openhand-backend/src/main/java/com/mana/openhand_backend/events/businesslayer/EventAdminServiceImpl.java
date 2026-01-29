package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EventAdminServiceImpl implements EventAdminService {

    private static final Logger logger = LoggerFactory.getLogger(EventAdminServiceImpl.class);

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final SendGridEmailService sendGridEmailService;
    private final NotificationService notificationService;
    private final EventCompletionService eventCompletionService;
    private final EventImageService eventImageService;

    public EventAdminServiceImpl(EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            SendGridEmailService sendGridEmailService,
            NotificationService notificationService,
            EventCompletionService eventCompletionService,
            EventImageService eventImageService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.sendGridEmailService = sendGridEmailService;
        this.notificationService = notificationService;
        this.eventCompletionService = eventCompletionService;
        this.eventImageService = eventImageService;
    }

    @Override
    public Event createEvent(CreateEventRequest request) {
        LocalDateTime startDateTime = request.getParsedStartDateTime();
        LocalDateTime endDateTime = request.getParsedEndDateTime();

        if (endDateTime != null && !endDateTime.isAfter(startDateTime)) {
            throw new IllegalArgumentException("endDateTime must be after startDateTime");
        }

        Integer maxCapacity = request.getMaxCapacity();
        Integer currentRegistrations = 0;

        EventStatus status = EventStatus.OPEN;
        if (maxCapacity != null) {
            if (maxCapacity <= 0) {
                throw new IllegalArgumentException("maxCapacity must be greater than 0");
            }
        }

        Event event = new Event(
                request.getTitle().trim(),
                request.getDescription().trim(),
                startDateTime,
                endDateTime,
                request.getLocationName().trim(),
                request.getAddress().trim(),
                status,
                maxCapacity,
                currentRegistrations,
                request.getCategory() != null && !request.getCategory().isBlank()
                        ? request.getCategory().trim()
                        : null);

        return eventRepository.save(event);
    }

    @Override
    @SuppressWarnings("null")
    public Event updateEvent(Long id, CreateEventRequest request) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id " + id));

        if (eventCompletionService.ensureCompletedIfEnded(existing, LocalDateTime.now())) {
            throw new IllegalArgumentException("Completed events cannot be edited.");
        }

        LocalDateTime originalStart = existing.getStartDateTime();

        LocalDateTime startDateTime = request.getParsedStartDateTime();
        LocalDateTime endDateTime = request.getParsedEndDateTime();

        if (endDateTime != null && !endDateTime.isAfter(startDateTime)) {
            throw new IllegalArgumentException("endDateTime must be after startDateTime");
        }

        Integer maxCapacity = request.getMaxCapacity();
        int currentCount = existing.getCurrentRegistrations() != null ? existing.getCurrentRegistrations() : 0;
        if (maxCapacity != null) {
            if (maxCapacity <= 0) {
                throw new IllegalArgumentException("maxCapacity must be greater than 0");
            }
            if (currentCount > maxCapacity) {
                throw new IllegalArgumentException("maxCapacity cannot be less than current registrations");
            }
        }

        existing.setTitle(request.getTitle().trim());
        existing.setDescription(request.getDescription().trim());
        existing.setStartDateTime(startDateTime);
        existing.setEndDateTime(endDateTime);
        existing.setLocationName(request.getLocationName().trim());
        existing.setAddress(request.getAddress().trim());
        existing.setCategory(
                request.getCategory() != null && !request.getCategory().isBlank()
                        ? request.getCategory().trim()
                        : null);
        existing.setMaxCapacity(maxCapacity);
        existing.setStatus(determineStatus(maxCapacity, currentCount));

        Event updated = eventRepository.save(existing);

        // Notify members if the schedule changed
        if (scheduleChanged(originalStart, updated.getStartDateTime())) {
            notifyScheduleChange(updated);
        }

        return updated;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Event cancelEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id " + id));

        if (event.getStatus() == EventStatus.CANCELLED) {
            return event;
        }

        event.setStatus(EventStatus.CANCELLED);
        Event cancelledEvent = eventRepository.save(event);

        // Cancel all active registrations
        List<Registration> registrations = registrationRepository.findByEventId(event.getId());
        List<Registration> cancelledRegistrations = registrations.stream()
                .filter(reg -> reg.getStatus() != RegistrationStatus.CANCELLED)
                .toList();

        cancelledRegistrations.forEach(reg -> reg.setStatus(RegistrationStatus.CANCELLED));
        registrationRepository.saveAll(cancelledRegistrations);

        // Notify all registered users (now cancelled)
        notifyCancellation(cancelledEvent, cancelledRegistrations);

        return cancelledEvent;
    }

    private boolean scheduleChanged(LocalDateTime originalStart, LocalDateTime updatedStart) {
        if (originalStart == null && updatedStart == null) {
            return false;
        }
        if (originalStart == null) {
            return true;
        }
        return !originalStart.equals(updatedStart);
    }

    private void notifyScheduleChange(Event event) {
        try {
            List<Registration> registrations = registrationRepository.findByEventId(event.getId());
            registrations.stream()
                    .filter(reg -> reg.getStatus() != RegistrationStatus.CANCELLED)
                    .forEach(reg -> {
                        String language = reg.getUser().getPreferredLanguage() != null
                                ? reg.getUser().getPreferredLanguage()
                                : "en";
                        try {
                            sendGridEmailService.sendCancellationOrUpdate(
                                    reg.getUser().getEmail(),
                                    reg.getUser().getName(),
                                    event.getTitle(),
                                    "Event schedule updated to " + event.getStartDateTime(),
                                    language);
                        } catch (Exception ex) {
                            logger.error("Failed to send schedule update email for registration {}: {}",
                                    reg.getId(), ex.getMessage());
                        }

                        // App Notification
                        try {
                            notificationService.createNotification(
                                    reg.getUser().getId(),
                                    event.getId(),
                                    "EVENT_UPDATE",
                                    language);
                        } catch (Exception e) {
                            logger.error("Failed notification for reg {}: {}", reg.getId(), e.getMessage());
                        }
                    });
        } catch (Exception ex) {
            logger.error("Failed to process schedule change notifications for event {}: {}", event.getId(),
                    ex.getMessage());
        }
    }

    private void notifyCancellation(Event event, List<Registration> recipients) {
        try {
            recipients.forEach(reg -> {
                String language = reg.getUser().getPreferredLanguage() != null
                        ? reg.getUser().getPreferredLanguage()
                        : "en";
                // Email
                try {
                    sendGridEmailService.sendCancellationOrUpdate(
                            reg.getUser().getEmail(),
                            reg.getUser().getName(),
                            event.getTitle(),
                            "Event Cancelled",
                            language);
                } catch (Exception e) {
                    logger.error("Failed email for reg {}: {}", reg.getId(), e.getMessage());
                }

                // App Notification
                try {
                    notificationService.createNotification(
                            reg.getUser().getId(),
                            event.getId(),
                            "CANCELLATION", // NotificationType name
                            language);
                } catch (Exception e) {
                    logger.error("Failed notification for reg {}: {}", reg.getId(), e.getMessage());
                }
            });
        } catch (Exception ex) {
            logger.error("Failed to notify cancellation for event {}", event.getId(), ex);
        }
    }

    private EventStatus determineStatus(Integer maxCapacity, Integer currentRegistrations) {
        int current = currentRegistrations != null ? currentRegistrations : 0;
        if (maxCapacity == null) {
            return EventStatus.OPEN;
        }
        if (current >= maxCapacity) {
            return EventStatus.FULL;
        }
        if (current >= Math.ceil(maxCapacity * 0.8)) {
            return EventStatus.NEARLY_FULL;
        }
        return EventStatus.OPEN;
    }

    @Override
    public com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse uploadEventImage(Long id,
            org.springframework.web.multipart.MultipartFile file, String baseUrl) {
        return eventImageService.storeEventImage(id, file, baseUrl);
    }

    @Override
    public com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse getEventImage(Long id,
            String baseUrl) {
        return eventImageService.getEventImage(id, baseUrl);
    }
}

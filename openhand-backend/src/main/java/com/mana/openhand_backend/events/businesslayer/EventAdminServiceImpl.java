package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class EventAdminServiceImpl implements EventAdminService {

    private static final Logger logger = LoggerFactory.getLogger(EventAdminServiceImpl.class);

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final SendGridEmailService sendGridEmailService;

    public EventAdminServiceImpl(EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            SendGridEmailService sendGridEmailService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.sendGridEmailService = sendGridEmailService;
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
                        : null
        );

        return eventRepository.save(event);
    }

    @Override    @SuppressWarnings("null")    public Event updateEvent(Long id, CreateEventRequest request) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id " + id));
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
                        : null
        );
        existing.setMaxCapacity(maxCapacity);
        existing.setStatus(determineStatus(maxCapacity, currentCount));

        Event updated = eventRepository.save(existing);

        // Notify members if the schedule changed
        if (scheduleChanged(originalStart, updated.getStartDateTime())) {
            notifyScheduleChange(updated);
        }

        return updated;
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
                        try {
                            String language = reg.getUser().getPreferredLanguage() != null
                                    ? reg.getUser().getPreferredLanguage()
                                    : "en";
                            sendGridEmailService.sendCancellationOrUpdate(
                                    reg.getUser().getEmail(),
                                    reg.getUser().getName(),
                                    event.getTitle(),
                                    "Event schedule updated to " + event.getStartDateTime(),
                                    language
                            );
                        } catch (Exception ex) {
                            logger.error("Failed to send schedule update email for registration {}: {}",
                                    reg.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            logger.error("Failed to process schedule change notifications for event {}: {}", event.getId(), ex.getMessage());
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
}

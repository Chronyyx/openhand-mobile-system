package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
public class EventAdminServiceImpl implements EventAdminService {

    private final EventRepository eventRepository;

    public EventAdminServiceImpl(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
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

    @Override
    public Event updateEvent(Long id, CreateEventRequest request) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id " + id));

        LocalDateTime startDateTime = request.getParsedStartDateTime();
        LocalDateTime endDateTime = request.getParsedEndDateTime();

        if (endDateTime != null && !endDateTime.isAfter(startDateTime)) {
            throw new IllegalArgumentException("endDateTime must be after startDateTime");
        }

        Integer maxCapacity = request.getMaxCapacity();
        if (maxCapacity != null) {
            if (maxCapacity <= 0) {
                throw new IllegalArgumentException("maxCapacity must be greater than 0");
            }
            if (existing.getCurrentRegistrations() != null
                    && existing.getCurrentRegistrations() > maxCapacity) {
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

        return eventRepository.save(existing);
    }
}

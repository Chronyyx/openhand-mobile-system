package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
}


package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventStaffServiceImpl implements EventStaffService {

    private final EventRepository eventRepository;
    private final EventCompletionService eventCompletionService;

    public EventStaffServiceImpl(EventRepository eventRepository,
                                 EventCompletionService eventCompletionService) {
        this.eventRepository = eventRepository;
        this.eventCompletionService = eventCompletionService;
    }

    @Override
    public List<Event> getEventsForStaff() {
        eventCompletionService.refreshCompletedEvents(LocalDateTime.now());
        return eventRepository.findAll(Sort.by(Sort.Direction.ASC, "startDateTime"));
    }

    @Override
    public Event markEventCompleted(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        return eventCompletionService.markCompleted(event);
    }
}

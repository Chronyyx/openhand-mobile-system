package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.businesslayer.EventService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.domainclientlayer.EventResponseModel;
import com.mana.openhand_backend.events.utils.EventResponseMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/upcoming")
    public List<EventResponseModel> getUpcomingEvents() {
        List<Event> events = eventService.getUpcomingEvents();
        return events.stream()
                .map(EventResponseMapper::toResponseModel)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EventResponseModel getEventById(@PathVariable Long id) {
        Event event = eventService.getEventById(id);
        return EventResponseMapper.toResponseModel(event);
    }
}

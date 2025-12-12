package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.businesslayer.EventAdminService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.domainclientlayer.EventResponseModel;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import com.mana.openhand_backend.events.utils.EventResponseMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/events")
@PreAuthorize("hasRole('ADMIN')")
public class EventAdminController {

    private final EventAdminService eventAdminService;

    public EventAdminController(EventAdminService eventAdminService) {
        this.eventAdminService = eventAdminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponseModel createEvent(@Valid @RequestBody CreateEventRequest request) {
        try {
            Event created = eventAdminService.createEvent(request);
            return EventResponseMapper.toResponseModel(created);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}


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

import java.util.NoSuchElementException;

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

    @PutMapping("/{id}")
    public EventResponseModel updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody CreateEventRequest request) {
        try {
            Event updated = eventAdminService.updateEvent(id, request);
            return EventResponseMapper.toResponseModel(updated);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    public EventResponseModel cancelEvent(@PathVariable Long id) {
        try {
            Event cancelled = eventAdminService.cancelEvent(id);
            return EventResponseMapper.toResponseModel(cancelled);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/{id}/image")
    public com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse uploadEventImage(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request) {
        String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();
        try {
            return eventAdminService.uploadEventImage(id, file, baseUrl);
        } catch (java.util.NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/{id}/image")
    public com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse getEventImage(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest request) {
        String baseUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath(null)
                .build()
                .toUriString();
        try {
            return eventAdminService.getEventImage(id, baseUrl);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}

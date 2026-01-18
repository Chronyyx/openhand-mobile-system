package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.businesslayer.EventStaffService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.domainclientlayer.EventResponseModel;
import com.mana.openhand_backend.events.utils.EventResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/employee/events")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class EventEmployeeController {

    private final EventStaffService eventStaffService;

    public EventEmployeeController(EventStaffService eventStaffService) {
        this.eventStaffService = eventStaffService;
    }

    @GetMapping
    public List<EventResponseModel> getEventsForStaff() {
        List<Event> events = eventStaffService.getEventsForStaff();
        return events.stream()
                .map(EventResponseMapper::toResponseModel)
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}/complete")
    public EventResponseModel markEventCompleted(@PathVariable Long id) {
        Event updated = eventStaffService.markEventCompleted(id);
        return EventResponseMapper.toResponseModel(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteArchivedEvent(@PathVariable Long id) {
        try {
            eventStaffService.deleteArchivedEvent(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }
}

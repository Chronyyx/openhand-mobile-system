package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.businesslayer.EventService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.domainclientlayer.EventAttendeesResponseModel;
import com.mana.openhand_backend.events.domainclientlayer.EventResponseModel;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;
import com.mana.openhand_backend.events.utils.EventResponseMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import com.mana.openhand_backend.registrations.domainclientlayer.GroupRegistrationRequestModel;
import com.mana.openhand_backend.registrations.domainclientlayer.GroupRegistrationResponseModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final RegistrationService registrationService;
    private final UserRepository userRepository;

    public EventController(EventService eventService,
            RegistrationService registrationService,
            UserRepository userRepository) {
        this.eventService = eventService;
        this.registrationService = registrationService;
        this.userRepository = userRepository;
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

    @GetMapping("/{id}/registration-summary")
    public RegistrationSummaryResponseModel getRegistrationSummary(@PathVariable Long id) {
        return eventService.getRegistrationSummary(id);
    }

    @GetMapping("/{id}/attendees")
    @PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
    public EventAttendeesResponseModel getEventAttendees(@PathVariable Long id) {
        return eventService.getEventAttendees(id);
    }

    @PostMapping("/{id}/registrations")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public GroupRegistrationResponseModel registerWithFamily(
            @PathVariable Long id,
            @RequestBody GroupRegistrationRequestModel request,
            Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = extractUserIdFromEmail(userDetails.getUsername());
        validateFamilyMembers(request.getFamilyMembers());
        return registrationService.registerForEventWithFamily(userId, id, request.getFamilyMembers());
    }

    private Long extractUserIdFromEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return user.getId();
    }

    private void validateFamilyMembers(List<com.mana.openhand_backend.registrations.domainclientlayer.FamilyMemberRequestModel> familyMembers) {
        if (familyMembers == null) {
            return;
        }
        for (com.mana.openhand_backend.registrations.domainclientlayer.FamilyMemberRequestModel member : familyMembers) {
            String name = member.getFullName() != null ? member.getFullName().trim() : "";
            boolean hasAge = member.getAge() != null;
            boolean hasDob = member.getDateOfBirth() != null && !member.getDateOfBirth().isBlank();
            if (name.isBlank() || (!hasAge && !hasDob)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Family members require fullName and age or dateOfBirth.");
            }
        }
    }
}

package com.mana.openhand_backend.registrations.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.businesslayer.UserMemberService;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryFilter;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationRequestModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationResponseModel;
import com.mana.openhand_backend.registrations.utils.RegistrationResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserMemberService userMemberService;

    public RegistrationController(RegistrationService registrationService, UserMemberService userMemberService) {
        this.registrationService = registrationService;
        this.userMemberService = userMemberService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_MEMBER') or hasRole('ROLE_EMPLOYEE')")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponseModel registerForEvent(
            @RequestBody RegistrationRequestModel request,
            Authentication authentication) {

        // Extract user ID from authentication
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = extractUserIdFromEmail(userDetails.getUsername());

        Registration registration = registrationService.registerForEvent(userId, request.getEventId());
        return RegistrationResponseMapper.toResponseModel(registration);
    }

    @GetMapping("/my-registrations")
    @PreAuthorize("hasRole('ROLE_MEMBER') or hasRole('ROLE_EMPLOYEE')")
    public List<RegistrationResponseModel> getMyRegistrations(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = extractUserIdFromEmail(userDetails.getUsername());

        List<Registration> registrations = registrationService.getUserRegistrations(userId);
        return registrations.stream()
                .map(RegistrationResponseMapper::toResponseModel)
                .collect(Collectors.toList());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public List<RegistrationHistoryResponseModel> getMyRegistrationHistory(
            Authentication authentication,
            @RequestParam(value = "filter", defaultValue = "ALL") String filter) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = extractUserIdFromEmail(userDetails.getUsername());

        RegistrationHistoryFilter parsedFilter = parseHistoryFilter(filter);
        return registrationService.getUserRegistrationHistory(userId, parsedFilter);
    }

    @DeleteMapping("/event/{eventId}")
    @PreAuthorize("hasRole('ROLE_MEMBER') or hasRole('ROLE_EMPLOYEE')")
    public RegistrationResponseModel cancelRegistration(
            @PathVariable Long eventId,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Long userId = extractUserIdFromEmail(userDetails.getUsername());

        Registration registration = registrationService.cancelRegistration(userId, eventId);
        return RegistrationResponseMapper.toResponseModel(registration);
    }

    // Helper method to extract user ID from email
    // Helper method to extract user ID from email
    private Long extractUserIdFromEmail(String email) {
        User user = userMemberService.getProfileByEmail(email);
        return user.getId();
    }

    private RegistrationHistoryFilter parseHistoryFilter(String filter) {
        try {
            return RegistrationHistoryFilter.valueOf(filter.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filter: " + filter);
        }
    }
}

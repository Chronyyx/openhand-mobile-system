package com.mana.openhand_backend.registrations.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.domainclientlayer.EmployeeRegistrationRequestModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationResponseModel;
import com.mana.openhand_backend.registrations.utils.RegistrationResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/employee/registrations")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class EmployeeRegistrationController {

    private final RegistrationService registrationService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public EmployeeRegistrationController(RegistrationService registrationService,
            NotificationService notificationService,
            UserRepository userRepository) {
        this.registrationService = registrationService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponseModel registerParticipant(@RequestBody EmployeeRegistrationRequestModel request,
            Authentication authentication) {
        Registration registration;
        try {
            registration = registrationService.registerForEvent(request.getUserId(), request.getEventId());
        } catch (EventNotFoundException ex) {
            // Treat missing event as a bad request for employee-driven registrations
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("User not found")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
            }
            throw ex;
        }

        // Notify the acting employee/admin that the participant was registered
        try {
            UserDetails actorDetails = (UserDetails) authentication.getPrincipal();
            User actor = userRepository.findByEmail(actorDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Acting user not found: " + actorDetails.getUsername()));

            // Get the participant who was registered
            User participant = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Participant not found: " + request.getUserId()));

            String language = actor.getPreferredLanguage() != null ? actor.getPreferredLanguage() : "en";
            notificationService.createNotification(
                    actor.getId(),
                    request.getEventId(),
                    "EMPLOYEE_REGISTERED_PARTICIPANT",
                    language,
                    participant.getName());

        } catch (Exception e) {
            // Do not block registration flow on notification failures
            System.err.println("Failed to notify actor about registration: " + e.getMessage());
        }

        return RegistrationResponseMapper.toResponseModel(registration);
    }
}

package com.mana.openhand_backend.notifications.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationPreferenceService;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceResponseModel;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceUpdateRequestModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final UserRepository userRepository;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService,
                                            UserRepository userRepository) {
        this.preferenceService = preferenceService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public NotificationPreferenceResponseModel getPreferences(Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        return preferenceService.getPreferencesForUser(userId);
    }

    @PutMapping
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public NotificationPreferenceResponseModel updatePreferences(
            Authentication authentication,
            @RequestBody NotificationPreferenceUpdateRequestModel request) {
        Long userId = extractUserIdFromAuth(authentication);
        return preferenceService.updatePreferences(userId, request);
    }

    private Long extractUserIdFromAuth(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userDetails.getUsername()));
        return user.getId();
    }
}

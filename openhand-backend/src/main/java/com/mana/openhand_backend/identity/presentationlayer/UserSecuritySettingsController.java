package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.SecuritySettingsResponse;
import com.mana.openhand_backend.identity.presentationlayer.payload.UpdateSecuritySettingsRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users/me/security-settings")
public class UserSecuritySettingsController {

    private final UserRepository userRepository;

    public UserSecuritySettingsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<SecuritySettingsResponse> getSecuritySettings() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(new SecuritySettingsResponse(user.isBiometricsEnabled()));
    }

    @PutMapping
    public ResponseEntity<SecuritySettingsResponse> updateSecuritySettings(
            @RequestBody UpdateSecuritySettingsRequest request) {
        User user = getAuthenticatedUser();
        user.setBiometricsEnabled(request.isBiometricsEnabled());
        User saved = userRepository.save(user);
        return ResponseEntity.ok(new SecuritySettingsResponse(saved.isBiometricsEnabled()));
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }
}

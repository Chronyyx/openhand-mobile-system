package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.businesslayer.ProfileImageStorageService;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel;
import com.mana.openhand_backend.identity.presentationlayer.payload.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserProfileController {
    private final UserRepository userRepository;
    private final ProfileImageStorageService profileImageStorageService;

    public UserProfileController(UserRepository userRepository,
            ProfileImageStorageService profileImageStorageService) {
        this.userRepository = userRepository;
        this.profileImageStorageService = profileImageStorageService;
    }

    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Unauthorized"));
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("User not found."));
        }

        ProfileImageStorageService.StoredProfileImage stored;
        try {
            stored = profileImageStorageService.storeProfileImage(file);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new MessageResponse(ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Unable to save profile image."));
        }

        String previousUrl = user.getProfileImageUrl();
        user.setProfileImageUrl(stored.url());
        try {
            userRepository.save(user);
        } catch (Exception ex) {
            profileImageStorageService.deleteByUrlIfPresent(stored.url());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Unable to update profile image."));
        }

        if (previousUrl != null && !previousUrl.isBlank()) {
            profileImageStorageService.deleteByUrlIfPresent(previousUrl);
        }

        return ResponseEntity.ok(UserResponseModel.fromEntity(user));
    }
}

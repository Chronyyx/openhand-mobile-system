package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.businesslayer.ProfilePictureService;
import com.mana.openhand_backend.identity.presentationlayer.payload.MessageResponse;
import com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse;
import com.mana.openhand_backend.security.services.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserProfileController {
        private static final Logger logger = LoggerFactory.getLogger(UserProfileController.class);

        private final ProfilePictureService profilePictureService;
        private final com.mana.openhand_backend.identity.businesslayer.UserMemberService userMemberService;

        public UserProfileController(ProfilePictureService profilePictureService,
                        com.mana.openhand_backend.identity.businesslayer.UserMemberService userMemberService) {
                this.profilePictureService = profilePictureService;
                this.userMemberService = userMemberService;
        }

        @GetMapping("/profile")
        public ResponseEntity<com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel> getProfile() {
                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal();

                var user = userMemberService.getProfile(userDetails.getId());
                return ResponseEntity
                                .ok(com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel
                                                .fromEntity(user));
        }

        @PatchMapping("/profile")
        public ResponseEntity<com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel> updateProfile(
                        @RequestBody com.mana.openhand_backend.identity.presentationlayer.payload.ProfileRequest request) {
                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal();

                var updatedUser = userMemberService.updateProfile(userDetails.getId(), request);
                return ResponseEntity
                                .ok(com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel
                                                .fromEntity(updatedUser));
        }

        @PostMapping("/profile-picture")
        public ResponseEntity<?> uploadProfilePicture(@RequestParam("file") MultipartFile file,
                        HttpServletRequest request) {
                try {
                        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                                        .getAuthentication()
                                        .getPrincipal();
                        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                        ProfilePictureResponse response = profilePictureService.storeProfilePicture(userDetails.getId(),
                                        file,
                                        baseUrl);
                        return ResponseEntity.ok(response);
                } catch (IllegalArgumentException ex) {
                        return ResponseEntity.badRequest().body(new MessageResponse("Error: " + ex.getMessage()));
                } catch (RuntimeException ex) {
                        logger.error("Profile picture upload failed: {}", ex.getMessage());
                        return ResponseEntity.internalServerError()
                                        .body(new MessageResponse("Error: Unable to upload profile picture."));
                }
        }

        @GetMapping("/profile-picture")
        public ResponseEntity<ProfilePictureResponse> getProfilePicture(HttpServletRequest request) {
                UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext()
                                .getAuthentication()
                                .getPrincipal();
                String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                ProfilePictureResponse response = profilePictureService.getProfilePicture(userDetails.getId(), baseUrl);
                return ResponseEntity.ok(response);
        }
}

package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.common.services.FileStorageService;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ProfilePictureService {
    private static final Logger logger = LoggerFactory.getLogger(ProfilePictureService.class);

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final Path uploadDir;
    private final long maxSizeBytes;

    public ProfilePictureService(UserRepository userRepository,
            FileStorageService fileStorageService,
            @Value("${openhand.app.profilePicturesDir:uploads/profile-pictures}") String uploadDir,
            @Value("${openhand.app.profilePictureMaxSizeBytes:5242880}") long maxSizeBytes) {
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
    }

    public ProfilePictureResponse getProfilePicture(Long userId, String baseUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        String resolvedUrl = fileStorageService.toPublicUrl(baseUrl, user.getProfilePictureUrl());
        return new ProfilePictureResponse(resolvedUrl);
    }

    public ProfilePictureResponse storeProfilePicture(Long userId, MultipartFile file, String baseUrl) {
        fileStorageService.validateImageFile(file, maxSizeBytes);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String previousUrl = user.getProfilePictureUrl();
        String filenameBase = "user-" + userId;

        try {
            String filename = fileStorageService.storeFile(file, uploadDir, filenameBase);
            String relativeUrl = "/uploads/profile-pictures/" + filename;

            user.setProfilePictureUrl(relativeUrl);
            userRepository.save(user);

            if (previousUrl != null && previousUrl.startsWith("/uploads/profile-pictures/")) {
                deletePreviousFile(previousUrl);
            }

            return new ProfilePictureResponse(fileStorageService.toPublicUrl(baseUrl, relativeUrl));
        } catch (RuntimeException ex) {
            logger.error("Failed to store profile picture for user {}: {}", userId, ex.getMessage());
            throw ex;
        }
    }

    public String toPublicUrl(String baseUrl, String relativePath) {
        return fileStorageService.toPublicUrl(baseUrl, relativePath);
    }

    private void deletePreviousFile(String previousUrl) {
        String filename = previousUrl.replace("/uploads/profile-pictures/", "");
        Path previousPath = uploadDir.resolve(filename).normalize();
        fileStorageService.deleteFile(previousPath);
    }
}

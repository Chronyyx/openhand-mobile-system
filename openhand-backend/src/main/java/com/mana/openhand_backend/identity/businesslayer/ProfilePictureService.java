package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProfilePictureService {
    private static final Logger logger = LoggerFactory.getLogger(ProfilePictureService.class);
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UserRepository userRepository;
    private final Path uploadDir;
    private final long maxSizeBytes;

    public ProfilePictureService(UserRepository userRepository,
                                 @Value("${openhand.app.profilePicturesDir:uploads/profile-pictures}") String uploadDir,
                                 @Value("${openhand.app.profilePictureMaxSizeBytes:5242880}") long maxSizeBytes) {
        this.userRepository = userRepository;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
    }

    public ProfilePictureResponse getProfilePicture(Long userId, String baseUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        String resolvedUrl = toPublicUrl(baseUrl, user.getProfilePictureUrl());
        return new ProfilePictureResponse(resolvedUrl);
    }

    public ProfilePictureResponse storeProfilePicture(Long userId, MultipartFile file, String baseUrl) {
        validateFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String previousUrl = user.getProfilePictureUrl();
        String extension = resolveExtension(file);
        String filename = "user-" + userId + "-" + UUID.randomUUID() + extension;
        Path targetPath = uploadDir.resolve(filename).normalize();

        try {
            Files.createDirectories(uploadDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativeUrl = "/uploads/profile-pictures/" + filename;
            user.setProfilePictureUrl(relativeUrl);
            userRepository.save(user);

            if (previousUrl != null && previousUrl.startsWith("/uploads/profile-pictures/")) {
                deletePreviousFile(previousUrl);
            }

            return new ProfilePictureResponse(toPublicUrl(baseUrl, relativeUrl));
        } catch (IOException ex) {
            logger.error("Failed to store profile picture for user {}: {}", userId, ex.getMessage());
            throw new IllegalStateException("Unable to store profile picture. Please try again.");
        } catch (RuntimeException ex) {
            cleanupFile(targetPath);
            throw ex;
        }
    }

    public String toPublicUrl(String baseUrl, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        String prefix = (baseUrl != null) ? baseUrl.replaceAll("/+$", "") : "";
        String path = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        return prefix + path;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided.");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("File is too large. Max size is " + maxSizeBytes + " bytes.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only JPEG, PNG, or WEBP images are allowed.");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf('.'));
            if (ext.length() <= 5) {
                return ext.toLowerCase(Locale.ROOT);
            }
        }

        String contentType = file.getContentType();
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".jpg";
    }

    private void deletePreviousFile(String previousUrl) {
        String filename = previousUrl.replace("/uploads/profile-pictures/", "");
        Path previousPath = uploadDir.resolve(filename).normalize();
        cleanupFile(previousPath);
    }

    private void cleanupFile(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException ex) {
            logger.warn("Failed to delete profile picture file {}: {}", path, ex.getMessage());
        }
    }
}

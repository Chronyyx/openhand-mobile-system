package com.mana.openhand_backend.identity.businesslayer;

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
import java.util.Map;
import java.util.UUID;

@Service
public class ProfileImageStorageService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileImageStorageService.class);
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private final Path storageDir;
    private final String urlPath;
    private final long maxSizeBytes;

    public ProfileImageStorageService(
            @Value("${openhand.app.profile-images.dir:./uploads/profile-pictures}") String storageDir,
            @Value("${openhand.app.profile-images.url-path:/uploads/profile-pictures}") String urlPath,
            @Value("${openhand.app.profile-images.max-size-bytes:5242880}") long maxSizeBytes) {
        this.storageDir = Paths.get(storageDir).toAbsolutePath().normalize();
        this.urlPath = normalizeUrlPath(urlPath);
        this.maxSizeBytes = maxSizeBytes;
    }

    public StoredProfileImage storeProfileImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required.");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("File too large.");
        }

        String contentType = file.getContentType();
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Invalid image type.");
        }

        Files.createDirectories(storageDir);
        String filename = UUID.randomUUID() + "." + extension;
        Path destination = storageDir.resolve(filename).normalize();
        if (!destination.startsWith(storageDir)) {
            throw new IllegalArgumentException("Invalid file path.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        return new StoredProfileImage(filename, buildUrl(filename));
    }

    public void deleteByUrlIfPresent(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return;
        }

        if (!profileImageUrl.startsWith(urlPath)) {
            return;
        }

        String filename = profileImageUrl.substring(urlPath.length());
        if (filename.startsWith("/")) {
            filename = filename.substring(1);
        }

        if (filename.isBlank()) {
            return;
        }

        Path target = storageDir.resolve(filename).normalize();
        if (!target.startsWith(storageDir)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            logger.warn("Failed to delete old profile image {}", target, ex);
        }
    }

    public String getUrlPathPrefix() {
        return urlPath;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    private String buildUrl(String filename) {
        return urlPath + "/" + filename;
    }

    private static String normalizeUrlPath(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            return "/uploads/profile-pictures";
        }
        String trimmed = urlPath.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    public record StoredProfileImage(String filename, String url) {
    }
}

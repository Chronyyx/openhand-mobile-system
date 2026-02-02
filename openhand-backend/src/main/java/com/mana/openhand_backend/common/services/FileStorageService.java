package com.mana.openhand_backend.common.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    public void validateImageFile(MultipartFile file, long maxSizeBytes) {
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

    public String resolveExtension(MultipartFile file) {
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

    private String sanitizeFilenameBase(String filenameBase) {
        if (filenameBase == null) {
            throw new IllegalArgumentException("Filename base must not be null.");
        }

        String trimmed = filenameBase.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Filename base must not be empty.");
        }

        // Allow only alphanumeric characters, underscores, and hyphens.
        String sanitized = trimmed.replaceAll("[^A-Za-z0-9_-]", "_");

        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Filename base is invalid after sanitization.");
        }

        return sanitized;
    }

    public String storeFile(MultipartFile file, Path uploadDir, String filenameBase) {
        String safeBase = sanitizeFilenameBase(filenameBase);
        String extension = resolveExtension(file);
        String filename = safeBase + "-" + UUID.randomUUID() + extension;

        Path normalizedUploadDir = uploadDir.toAbsolutePath().normalize();
        Path targetPath = normalizedUploadDir.resolve(filename).normalize();

        if (!targetPath.startsWith(normalizedUploadDir)) {
            throw new IllegalArgumentException("Invalid filename: path traversal is not allowed.");
        }

        try {
            Files.createDirectories(normalizedUploadDir);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return filename;
        } catch (IOException ex) {
            logger.error("Failed to store file {}: {}", filename, ex.getMessage());
            cleanupFile(targetPath);
            throw new IllegalStateException("Unable to store file. Please try again.");
        } catch (RuntimeException ex) {
            // Try to cleanup if something went wrong
            cleanupFile(targetPath);
            throw ex;
        }
    }

    public void deleteFile(Path filePath) {
        cleanupFile(filePath);
    }

    public void cleanupFile(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException ex) {
            logger.warn("Failed to delete file {}: {}", path, ex.getMessage());
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
}

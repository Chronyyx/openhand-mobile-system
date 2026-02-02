package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.common.presentationlayer.payload.ImageUrlResponse;
import com.mana.openhand_backend.common.services.FileStorageService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

@Service
public class EventImageService {
    private static final Logger logger = LoggerFactory.getLogger(EventImageService.class);

    private static final String PUBLIC_URL_PREFIX = "/uploads/event-images/";

    private final EventRepository eventRepository;
    private final FileStorageService fileStorageService;
    private final Path uploadDir;
    private final long maxSizeBytes;

    public EventImageService(EventRepository eventRepository,
            FileStorageService fileStorageService,
            @Value("${openhand.app.eventImagesDir:uploads/event-images}") String uploadDir,
            @Value("${openhand.app.eventImageMaxSizeBytes:5242880}") long maxSizeBytes) {
        this.eventRepository = eventRepository;
        this.fileStorageService = fileStorageService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
    }

    // Since EventImageService is now implementing EventAdminService, it must
    // implement all methods.
    // However, it seems it was designed as a component used BY EventAdminService or
    // similar.
    // Wait, let's check the previous imports. It was NOT implementing
    // EventAdminService before?
    // The previous file content shows `public class EventImageService {`. It did
    // NOT implement `EventAdminService`.
    // BUT `EventAdminServiceImpl` likely implements `EventAdminService` and
    // delegates to `EventImageService`.
    // I need to check `EventAdminServiceImpl` too!

    // I will revert adding `implements EventAdminService` here if it wasn't there.
    // The previous code snippet in Step 482 does not show `implements
    // EventAdminService`.
    // So I should keep it as is, just change return types.

    public ImageUrlResponse getEventImage(Long eventId, String baseUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));
        String resolvedUrl = fileStorageService.toPublicUrl(baseUrl, event.getImageUrl());
        return new ImageUrlResponse(resolvedUrl);
    }

    public ImageUrlResponse storeEventImage(Long eventId, MultipartFile file, String baseUrl) {
        fileStorageService.validateImageFile(file, maxSizeBytes);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

        String previousUrl = event.getImageUrl();
        String filenameBase = "event-" + eventId;

        String filename = fileStorageService.storeFile(file, uploadDir, filenameBase);
        try {
            String relativeUrl = PUBLIC_URL_PREFIX + filename;

            event.setImageUrl(relativeUrl);
            eventRepository.save(event);

            if (previousUrl != null && previousUrl.startsWith(PUBLIC_URL_PREFIX)) {
                deletePreviousFile(previousUrl);
            }

            return new ImageUrlResponse(fileStorageService.toPublicUrl(baseUrl, relativeUrl));
        } catch (RuntimeException ex) {
            logger.error("Failed to store image for event {}: {}", eventId, ex.getMessage());
            // Cleanup orphaned file
            fileStorageService.deleteFile(uploadDir.resolve(filename).normalize());
            throw ex;
        }
    }

    private void deletePreviousFile(String previousUrl) {
        String filename = previousUrl.replace(PUBLIC_URL_PREFIX, "");
        Path previousPath = uploadDir.resolve(filename).normalize();
        fileStorageService.deleteFile(previousPath);
    }
}

package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.common.services.FileStorageService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse;
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

    private final EventRepository eventRepository;
    private final FileStorageService fileStorageService;
    private final Path uploadDir;
    private final long maxSizeBytes;

    public EventImageService(EventRepository eventRepository,
            FileStorageService fileStorageService,
            @Value("${openhand.app.eventImagesDir:uploads/event-images}") String uploadDir,
            @Value("${openhand.app.profilePictureMaxSizeBytes:5242880}") long maxSizeBytes) {
        this.eventRepository = eventRepository;
        this.fileStorageService = fileStorageService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxSizeBytes = maxSizeBytes;
    }

    public ProfilePictureResponse getEventImage(Long eventId, String baseUrl) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));
        String resolvedUrl = fileStorageService.toPublicUrl(baseUrl, event.getImageUrl());
        return new ProfilePictureResponse(resolvedUrl);
    }

    public ProfilePictureResponse storeEventImage(Long eventId, MultipartFile file, String baseUrl) {
        fileStorageService.validateImageFile(file, maxSizeBytes);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

        String previousUrl = event.getImageUrl();
        String filenameBase = "event-" + eventId;

        try {
            String filename = fileStorageService.storeFile(file, uploadDir, filenameBase);
            String relativeUrl = "/uploads/event-images/" + filename;

            event.setImageUrl(relativeUrl);
            eventRepository.save(event);

            if (previousUrl != null && previousUrl.startsWith("/uploads/event-images/")) {
                deletePreviousFile(previousUrl);
            }

            return new ProfilePictureResponse(fileStorageService.toPublicUrl(baseUrl, relativeUrl));
        } catch (RuntimeException ex) {
            logger.error("Failed to store image for event {}: {}", eventId, ex.getMessage());
            throw ex;
        }
    }

    private void deletePreviousFile(String previousUrl) {
        String filename = previousUrl.replace("/uploads/event-images/", "");
        Path previousPath = uploadDir.resolve(filename).normalize();
        fileStorageService.deleteFile(previousPath);
    }
}

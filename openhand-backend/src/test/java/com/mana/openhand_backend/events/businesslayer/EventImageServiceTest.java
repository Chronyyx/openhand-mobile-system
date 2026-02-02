package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.common.presentationlayer.payload.ImageUrlResponse;
import com.mana.openhand_backend.common.services.FileStorageService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventImageServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private FileStorageService fileStorageService;

    private EventImageService eventImageService;
    private final String uploadDir = "uploads/event-images";
    private final long maxSizeBytes = 1000;

    @BeforeEach
    void setUp() {
        eventImageService = new EventImageService(eventRepository, fileStorageService, uploadDir, maxSizeBytes);
    }

    private Event createEvent() {
        return new Event("Title", "Desc", LocalDateTime.now(), null, "Loc", "Addr", null, 10, 0, "Cat");
    }

    @Test
    void getEventImage_success() {
        Long eventId = 1L;
        String baseUrl = "http://localhost:8080";
        String imageUrl = "/uploads/event-images/test.jpg";
        Event event = createEvent();
        event.setImageUrl(imageUrl);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(fileStorageService.toPublicUrl(baseUrl, imageUrl))
                .thenReturn("http://localhost:8080/uploads/event-images/test.jpg");

        ImageUrlResponse response = eventImageService.getEventImage(eventId, baseUrl);

        assertEquals("http://localhost:8080/uploads/event-images/test.jpg", response.getUrl());
    }

    @Test
    void getEventImage_notFound_throwsException() {
        Long eventId = 1L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> eventImageService.getEventImage(eventId, "url"));
    }

    @Test
    void storeEventImage_success_andDeletesPrevious() {
        Long eventId = 1L;
        String baseUrl = "http://localhost:8080";
        MultipartFile file = mock(MultipartFile.class);
        Event event = createEvent();
        event.setImageUrl("/uploads/event-images/old.jpg");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(fileStorageService.storeFile(eq(file), any(Path.class), anyString())).thenReturn("new.jpg");
        when(fileStorageService.toPublicUrl(anyString(), anyString())).thenReturn("full_url");

        eventImageService.storeEventImage(eventId, file, baseUrl);

        verify(fileStorageService).validateImageFile(file, maxSizeBytes);
        verify(eventRepository).save(event);
        verify(fileStorageService).deleteFile(any(Path.class)); // Verifies previous file deletion
        assertEquals("/uploads/event-images/new.jpg", event.getImageUrl());
    }

    @Test
    void storeEventImage_saveFailure_cleansUpOrphan() {
        Long eventId = 1L;
        MultipartFile file = mock(MultipartFile.class);
        Event event = createEvent();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(fileStorageService.storeFile(eq(file), any(Path.class), anyString())).thenReturn("new.jpg");
        doThrow(new RuntimeException("DB Error")).when(eventRepository).save(event);

        assertThrows(RuntimeException.class, () -> eventImageService.storeEventImage(eventId, file, "url"));

        verify(fileStorageService).deleteFile(argThat(path -> path.toString().endsWith("new.jpg")));
    }
}

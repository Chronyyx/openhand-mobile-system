package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAdminServiceImplTest {

    @Mock
    private EventRepository eventRepository;

@InjectMocks
    private EventAdminServiceImpl eventAdminService;

    private CreateEventRequest buildRequest(String title, String endDateTime, Integer maxCapacity, String category) {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle(title);
        request.setDescription("Desc");
        request.setStartDateTime("2026-01-01T18:00");
        request.setEndDateTime(endDateTime);
        request.setLocationName("Loc");
        request.setAddress("Addr");
        request.setMaxCapacity(maxCapacity);
        request.setCategory(category);
        return request;
    }

    @Test
    void createEvent_withValidInput_trimsValuesAndPersists() {
        CreateEventRequest request = buildRequest("  Title  ", "2026-01-01T20:00", 25, "  GENERAL ");
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventAdminService.createEvent(request);

        assertEquals("Title", result.getTitle());
        assertEquals("Desc", result.getDescription());
        assertEquals(LocalDateTime.parse("2026-01-01T18:00"), result.getStartDateTime());
        assertEquals(LocalDateTime.parse("2026-01-01T20:00"), result.getEndDateTime());
        assertEquals("Loc", result.getLocationName());
        assertEquals("Addr", result.getAddress());
        assertEquals(EventStatus.OPEN, result.getStatus());
        assertEquals(Integer.valueOf(25), result.getMaxCapacity());
        assertEquals(Integer.valueOf(0), result.getCurrentRegistrations());
        assertEquals("GENERAL", result.getCategory());

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertEquals("Title", saved.getValue().getTitle());
    }

    @Test
    void createEvent_withBlankCategory_setsCategoryToNull() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 10, "   ");
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventAdminService.createEvent(request);

        assertNull(result.getCategory());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void createEvent_withEndBeforeStart_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T17:00", 10, "GENERAL");

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.createEvent(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void createEvent_withNonPositiveCapacity_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 0, "GENERAL");

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.createEvent(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    private Event existingEvent(int currentRegistrations) {
        Event event = new Event(
                "Existing",
                "Existing desc",
                LocalDateTime.parse("2026-01-01T18:00"),
                LocalDateTime.parse("2026-01-01T20:00"),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                50,
                currentRegistrations,
                "GENERAL"
        );
        return event;
    }

    @Test
    void updateEvent_whenNotFound_throwsNoSuchElementException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 10, "GENERAL");
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> eventAdminService.updateEvent(99L, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_withEndBeforeStart_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T17:00", 10, "GENERAL");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent(3)));

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.updateEvent(1L, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_withMaxCapacityLessThanCurrent_throwsException() {
        CreateEventRequest request = buildRequest("Title", "2026-01-01T20:00", 2, "GENERAL");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent(3)));

        assertThrows(IllegalArgumentException.class, () -> eventAdminService.updateEvent(1L, request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_setsStatusToFullWhenAtCapacity() {
        CreateEventRequest request = buildRequest("Updated", "2026-01-02T20:00", 5, "GENERAL");
        Event existing = existingEvent(5);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventAdminService.updateEvent(1L, request);

        assertEquals(EventStatus.FULL, updated.getStatus());
        assertEquals(Integer.valueOf(5), updated.getMaxCapacity());
        assertEquals("Updated", updated.getTitle());
        verify(eventRepository, times(1)).save(existing);
    }

    @Test
    void updateEvent_setsStatusToNearlyFullAtEightyPercent() {
        CreateEventRequest request = buildRequest("Updated", "2026-01-02T20:00", 5, "GENERAL");
        Event existing = existingEvent(4);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updated = eventAdminService.updateEvent(1L, request);

        assertEquals(EventStatus.NEARLY_FULL, updated.getStatus());
        assertEquals(Integer.valueOf(5), updated.getMaxCapacity());
        verify(eventRepository, times(1)).save(existing);
    }
}

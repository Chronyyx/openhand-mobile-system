package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventCompletionServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventCompletionService eventCompletionService;

    @Test
    void refreshCompletedEvents_whenEmpty_returnsWithoutSaving() {
        when(eventRepository.findByEndDateTimeNotNullAndEndDateTimeLessThanEqualAndStatusNot(
                any(LocalDateTime.class), eq(EventStatus.COMPLETED)))
                .thenReturn(List.of());

        eventCompletionService.refreshCompletedEvents(LocalDateTime.now());

        verify(eventRepository, never()).saveAll(any());
    }

    @Test
    void refreshCompletedEvents_marksEventsCompletedAndSaves() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                0,
                "General");
        when(eventRepository.findByEndDateTimeNotNullAndEndDateTimeLessThanEqualAndStatusNot(
                any(LocalDateTime.class), eq(EventStatus.COMPLETED)))
                .thenReturn(List.of(event));

        eventCompletionService.refreshCompletedEvents(LocalDateTime.now());

        assertEquals(EventStatus.COMPLETED, event.getStatus());
        verify(eventRepository).saveAll(List.of(event));
    }

    @Test
    void ensureCompletedIfEnded_whenAlreadyCompleted_returnsTrue() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                "Loc",
                "Addr",
                EventStatus.COMPLETED,
                10,
                0,
                "General");

        assertTrue(eventCompletionService.ensureCompletedIfEnded(event, LocalDateTime.now()));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void ensureCompletedIfEnded_whenEndDatePassed_marksCompleted() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                0,
                "General");

        assertTrue(eventCompletionService.ensureCompletedIfEnded(event, LocalDateTime.now()));
        assertEquals(EventStatus.COMPLETED, event.getStatus());
        verify(eventRepository).save(event);
    }

    @Test
    void ensureCompletedIfEnded_whenNoEndDate_returnsFalse() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().plusDays(1),
                null,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                0,
                "General");

        assertFalse(eventCompletionService.ensureCompletedIfEnded(event, LocalDateTime.now()));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void markCompleted_whenAlreadyCompleted_returnsSame() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                "Loc",
                "Addr",
                EventStatus.COMPLETED,
                10,
                0,
                "General");

        Event result = eventCompletionService.markCompleted(event);

        assertSame(event, result);
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void markCompleted_whenActive_setsCompletedAndSaves() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().minusDays(2),
                LocalDateTime.now().minusDays(1),
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                0,
                "General");
        when(eventRepository.save(event)).thenReturn(event);

        Event result = eventCompletionService.markCompleted(event);

        assertEquals(EventStatus.COMPLETED, result.getStatus());
        verify(eventRepository).save(event);
    }
}

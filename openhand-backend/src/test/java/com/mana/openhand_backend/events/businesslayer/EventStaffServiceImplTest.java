package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventStaffServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventCompletionService eventCompletionService;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private EventStaffServiceImpl eventStaffService;

    @Test
    void deleteArchivedEvent_whenCompleted_deletesDependenciesAndEvent() {
        Long eventId = 42L;
        Event event = buildEvent(EventStatus.COMPLETED);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        eventStaffService.deleteArchivedEvent(eventId);

        verify(notificationRepository).deleteByEventId(eventId);
        verify(registrationRepository).deleteByEventId(eventId);
        verify(eventRepository).delete(event);
    }

    @Test
    void deleteArchivedEvent_whenNotCompleted_throwsAndDoesNotDelete() {
        Long eventId = 84L;
        Event event = buildEvent(EventStatus.OPEN);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> eventStaffService.deleteArchivedEvent(eventId)
        );

        assertEquals("Only completed events can be deleted.", ex.getMessage());
        verifyNoInteractions(notificationRepository, registrationRepository);
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void deleteArchivedEvent_whenMissing_throwsNotFound() {
        Long eventId = 99L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventStaffService.deleteArchivedEvent(eventId));

        verifyNoInteractions(notificationRepository, registrationRepository);
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void getEventsForStaff_refreshesAndSorts() {
        Event event = buildEvent(EventStatus.OPEN);
        when(eventRepository.findAll(eq(org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "startDateTime"))))
                .thenReturn(List.of(event));

        List<Event> result = eventStaffService.getEventsForStaff();

        assertEquals(1, result.size());
        verify(eventCompletionService).refreshCompletedEvents(any(LocalDateTime.class));
    }

    @Test
    void markEventCompleted_whenMissing_throwsNotFound() {
        when(eventRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventStaffService.markEventCompleted(10L));
    }

    @Test
    void markEventCompleted_delegatesToCompletionService() {
        Event event = buildEvent(EventStatus.OPEN);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventCompletionService.markCompleted(event)).thenReturn(event);

        Event result = eventStaffService.markEventCompleted(10L);

        assertEquals(event, result);
        verify(eventCompletionService).markCompleted(event);
    }

    private Event buildEvent(EventStatus status) {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);
        return new Event(
                "Test Event",
                "Test Description",
                start,
                end,
                "Test Location",
                "Test Address",
                status,
                10,
                0,
                "Test"
        );
    }
}

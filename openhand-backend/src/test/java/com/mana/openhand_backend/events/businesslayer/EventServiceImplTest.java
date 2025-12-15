package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void getUpcomingEvents_returnsEventsFromRepository() {
        // arrange
        // arrange
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        List<Event> events = Arrays.asList(event1, event2);


        when(eventRepository.findByStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(any(LocalDateTime.class)))
                .thenReturn(events);

        // act
        List<Event> result = eventService.getUpcomingEvents();

        // assert
        assertEquals(events, result);
        verify(eventRepository, times(1))
                .findByStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(any(LocalDateTime.class));
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void getEventById_whenEventExists_returnsEvent() {
        // arrange
        Long id = 1L;
        Event event = mock(Event.class);
        when(eventRepository.findById(id)).thenReturn(Optional.of(event));

        // act
        Event result = eventService.getEventById(id);

        // assert
        assertSame(event, result);
        verify(eventRepository, times(1)).findById(id);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void getEventById_whenEventDoesNotExist_throwsEventNotFoundException() {
        // arrange
        Long id = 99L;
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(EventNotFoundException.class, () -> eventService.getEventById(id));
        verify(eventRepository, times(1)).findById(id);
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void getRegistrationSummary_withCurrentRegistrationsSet_usesCurrentRegistrations() {
        // arrange
        Long eventId = 1L;
        Event event = mock(Event.class);
        when(event.getMaxCapacity()).thenReturn(100);
        when(event.getCurrentRegistrations()).thenReturn(40);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED)).thenReturn(5L);

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(eventId);

        // assert
        assertNotNull(result);
        assertEquals(eventId, result.getEventId());
        assertEquals(40, result.getTotalRegistrations());
        assertEquals(5, result.getWaitlistedCount());
        assertEquals(100, result.getMaxCapacity());
        assertEquals(60, result.getRemainingSpots());
        assertEquals(40.0, result.getPercentageFull());

        verify(eventRepository, times(1)).findById(eventId);
        verify(registrationRepository, times(1)).countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
        verifyNoMoreInteractions(eventRepository, registrationRepository);
    }

    @Test
    void getRegistrationSummary_withNullCurrentRegistrations_countsFromDatabase() {
        // arrange
        Long eventId = 2L;
        Event event = mock(Event.class);
        when(event.getMaxCapacity()).thenReturn(50);
        when(event.getCurrentRegistrations()).thenReturn(null);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED)).thenReturn(15L);
        when(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED)).thenReturn(3L);

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(eventId);

        // assert
        assertNotNull(result);
        assertEquals(eventId, result.getEventId());
        assertEquals(15, result.getTotalRegistrations());
        assertEquals(3, result.getWaitlistedCount());
        assertEquals(50, result.getMaxCapacity());
        assertEquals(35, result.getRemainingSpots());
        assertEquals(30.0, result.getPercentageFull());

        verify(eventRepository, times(1)).findById(eventId);
        verify(registrationRepository, times(1)).countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED);
        verify(registrationRepository, times(1)).countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
        verifyNoMoreInteractions(eventRepository, registrationRepository);
    }

    @Test
    void getRegistrationSummary_withUnlimitedCapacity_returnsNullRemainingSpots() {
        // arrange
        Long eventId = 3L;
        Event event = mock(Event.class);
        when(event.getMaxCapacity()).thenReturn(null);
        when(event.getCurrentRegistrations()).thenReturn(25);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED)).thenReturn(2L);

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(eventId);

        // assert
        assertNotNull(result);
        assertEquals(eventId, result.getEventId());
        assertEquals(25, result.getTotalRegistrations());
        assertEquals(2, result.getWaitlistedCount());
        assertNull(result.getMaxCapacity());
        assertNull(result.getRemainingSpots());
        assertNull(result.getPercentageFull());

        verify(eventRepository, times(1)).findById(eventId);
        verify(registrationRepository, times(1)).countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
        verifyNoMoreInteractions(eventRepository, registrationRepository);
    }

    @Test
    void getRegistrationSummary_withFullEvent_returnsZeroRemainingSpots() {
        // arrange
        Long eventId = 4L;
        Event event = mock(Event.class);
        when(event.getMaxCapacity()).thenReturn(100);
        when(event.getCurrentRegistrations()).thenReturn(100);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED)).thenReturn(10L);

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(eventId);

        // assert
        assertNotNull(result);
        assertEquals(eventId, result.getEventId());
        assertEquals(100, result.getTotalRegistrations());
        assertEquals(10, result.getWaitlistedCount());
        assertEquals(100, result.getMaxCapacity());
        assertEquals(0, result.getRemainingSpots());
        assertEquals(100.0, result.getPercentageFull());

        verify(eventRepository, times(1)).findById(eventId);
        verify(registrationRepository, times(1)).countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
        verifyNoMoreInteractions(eventRepository, registrationRepository);
    }

    @Test
    void getRegistrationSummary_withNoRegistrations_returnsZeroCounts() {
        // arrange
        Long eventId = 5L;
        Event event = mock(Event.class);
        when(event.getMaxCapacity()).thenReturn(75);
        when(event.getCurrentRegistrations()).thenReturn(0);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED)).thenReturn(0L);

        // act
        RegistrationSummaryResponseModel result = eventService.getRegistrationSummary(eventId);

        // assert
        assertNotNull(result);
        assertEquals(eventId, result.getEventId());
        assertEquals(0, result.getTotalRegistrations());
        assertEquals(0, result.getWaitlistedCount());
        assertEquals(75, result.getMaxCapacity());
        assertEquals(75, result.getRemainingSpots());
        assertEquals(0.0, result.getPercentageFull());

        verify(eventRepository, times(1)).findById(eventId);
        verify(registrationRepository, times(1)).countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED);
        verifyNoMoreInteractions(eventRepository, registrationRepository);
    }

    @Test
    void getRegistrationSummary_whenEventNotFound_throwsEventNotFoundException() {
        // arrange
        Long eventId = 999L;
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // act + assert
        assertThrows(EventNotFoundException.class, () -> eventService.getRegistrationSummary(eventId));
        verify(eventRepository, times(1)).findById(eventId);
        verifyNoMoreInteractions(eventRepository, registrationRepository);
    }
}

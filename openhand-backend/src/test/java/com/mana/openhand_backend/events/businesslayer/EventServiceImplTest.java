package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
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

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void getUpcomingEvents_returnsEventsFromRepository() {
        // arrange
        // arrange
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        List<Event> events = Arrays.asList(event1, event2);


        when(eventRepository.findByStartDateTimeAfterOrderByStartDateTimeAsc(any(LocalDateTime.class)))
                .thenReturn(events);

        // act
        List<Event> result = eventService.getUpcomingEvents();

        // assert
        assertEquals(events, result);
        verify(eventRepository, times(1))
                .findByStartDateTimeAfterOrderByStartDateTimeAsc(any(LocalDateTime.class));
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
}

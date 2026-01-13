package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceImplTest {

    private EventRepository eventRepository;
    private RegistrationRepository registrationRepository;
    private EventServiceImpl eventService;

    @BeforeEach
    void setup() {
        eventRepository = mock(EventRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        eventService = new EventServiceImpl(eventRepository, registrationRepository);
    }

    @Test
    void getUpcomingEvents_returnsFutureEventsWhenPresent() {
        Event future = new Event(
                "Future",
                "Desc",
                LocalDateTime.now().plusDays(1),
                null,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                0,
                null
        );
        when(eventRepository.findByStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(any()))
                .thenReturn(List.of(future));

        List<Event> results = eventService.getUpcomingEvents();

        assertThat(results).containsExactly(future);
        verify(eventRepository, never()).findAll(any(Sort.class));
    }

    @Test
    void getUpcomingEvents_fallsBackToAllWhenNoFuture() {
        Event past = new Event(
                "Past",
                "Desc",
                LocalDateTime.now().minusDays(2),
                null,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                5,
                null
        );
        when(eventRepository.findByStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(any()))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findAll(any(Sort.class))).thenReturn(List.of(past));

        List<Event> results = eventService.getUpcomingEvents();

        assertThat(results).containsExactly(past);
        verify(eventRepository).findAll(any(Sort.class));
    }
}

package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceImplTest {

    private EventRepository eventRepository;
    private RegistrationRepository registrationRepository;
    private EventCompletionService eventCompletionService;
    private EventServiceImpl eventService;

    @BeforeEach
    void setup() {
        eventRepository = mock(EventRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        eventCompletionService = mock(EventCompletionService.class);
        eventService = new EventServiceImpl(eventRepository, registrationRepository, eventCompletionService);
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
        when(eventRepository.findByStartDateTimeGreaterThanEqualAndStatusNotOrderByStartDateTimeAsc(
                any(),
                eq(EventStatus.COMPLETED)
        ))
                .thenReturn(List.of(future));

        List<Event> results = eventService.getUpcomingEvents();

        assertThat(results).containsExactly(future);
        verify(eventRepository, never()).findByStatusNotOrderByStartDateTimeAsc(EventStatus.COMPLETED);
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
        when(eventRepository.findByStartDateTimeGreaterThanEqualAndStatusNotOrderByStartDateTimeAsc(
                any(),
                eq(EventStatus.COMPLETED)
        ))
                .thenReturn(Collections.emptyList());
        when(eventRepository.findByStatusNotOrderByStartDateTimeAsc(EventStatus.COMPLETED))
                .thenReturn(List.of(past));

        List<Event> results = eventService.getUpcomingEvents();

        assertThat(results).containsExactly(past);
        verify(eventRepository).findByStatusNotOrderByStartDateTimeAsc(EventStatus.COMPLETED);
    }

    @Test
    void getRegistrationSummary_usesEventCountersWhenAvailable() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().plusDays(1),
                null,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                4,
                null
        );
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(1L, RegistrationStatus.WAITLISTED))
                .thenReturn(2L);

        RegistrationSummaryResponseModel summary = eventService.getRegistrationSummary(1L);

        assertEquals(4, summary.getTotalRegistrations());
        assertEquals(2, summary.getWaitlistedCount());
        assertEquals(10, summary.getMaxCapacity());
        assertEquals(6, summary.getRemainingSpots());
        assertEquals(40.0, summary.getPercentageFull());
        verify(registrationRepository, never())
                .countByEventIdAndStatus(1L, RegistrationStatus.CONFIRMED);
    }

    @Test
    void getRegistrationSummary_fallsBackToDbCountWhenEventCounterMissing() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().plusDays(1),
                null,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                0,
                null,
                null
        );
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(2L, RegistrationStatus.CONFIRMED))
                .thenReturn(3L);
        when(registrationRepository.countByEventIdAndStatus(2L, RegistrationStatus.WAITLISTED))
                .thenReturn(1L);

        RegistrationSummaryResponseModel summary = eventService.getRegistrationSummary(2L);

        assertEquals(3, summary.getTotalRegistrations());
        assertEquals(1, summary.getWaitlistedCount());
        assertEquals(0, summary.getMaxCapacity());
        assertEquals(0, summary.getRemainingSpots());
        assertEquals(0.0, summary.getPercentageFull());
    }

    @Test
    void getRegistrationSummary_withNoCapacityLeavesRemainingAndPercentageNull() {
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.now().plusDays(1),
                null,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                null,
                2,
                null
        );
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));
        when(registrationRepository.countByEventIdAndStatus(3L, RegistrationStatus.WAITLISTED))
                .thenReturn(0L);

        RegistrationSummaryResponseModel summary = eventService.getRegistrationSummary(3L);

        assertEquals(2, summary.getTotalRegistrations());
        assertNull(summary.getMaxCapacity());
        assertNull(summary.getRemainingSpots());
        assertNull(summary.getPercentageFull());
    }
}

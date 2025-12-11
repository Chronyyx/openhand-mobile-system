package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.businesslayer.EventService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.domainclientlayer.EventResponseModel;
import com.mana.openhand_backend.events.domainclientlayer.RegistrationSummaryResponseModel;
import com.mana.openhand_backend.events.utils.EventResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private Event createEvent(String title) {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 21, 0);

        return new Event(
                title,
                "Description " + title,
                start,
                end,
                "Location " + title,
                "Address " + title,
                EventStatus.OPEN,
                100,
                10,
                "General");
    }

    @Test
    void getUpcomingEvents_returnsMappedResponseModels() {
        // arrange
        Event event1 = createEvent("Event 1");
        Event event2 = createEvent("Event 2");
        List<Event> events = Arrays.asList(event1, event2);

        when(eventService.getUpcomingEvents()).thenReturn(events);

        // act
        List<EventResponseModel> result = eventController.getUpcomingEvents();

        // assert
        assertEquals(2, result.size());

        EventResponseModel expected1 = EventResponseMapper.toResponseModel(event1);
        EventResponseModel expected2 = EventResponseMapper.toResponseModel(event2);

        assertEquals(expected1.getTitle(), result.get(0).getTitle());
        assertEquals(expected2.getTitle(), result.get(1).getTitle());
        assertEquals(expected1.getLocationName(), result.get(0).getLocationName());
        assertEquals(expected2.getLocationName(), result.get(1).getLocationName());

        verify(eventService, times(1)).getUpcomingEvents();
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void getEventById_returnsMappedResponseModel() {
        // arrange
        Long id = 1L;
        Event event = createEvent("Single Event");

        when(eventService.getEventById(id)).thenReturn(event);

        // act
        EventResponseModel result = eventController.getEventById(id);

        // assert
        EventResponseModel expected = EventResponseMapper.toResponseModel(event);

        assertNotNull(result);
        assertEquals(expected.getTitle(), result.getTitle());
        assertEquals(expected.getDescription(), result.getDescription());
        assertEquals(expected.getLocationName(), result.getLocationName());
        assertEquals(expected.getStatus(), result.getStatus());

        verify(eventService, times(1)).getEventById(id);
        verifyNoMoreInteractions(eventService);
    }

    @Test
    void getRegistrationSummary_returnsRegistrationSummaryFromService() {
        // arrange
        Long eventId = 1L;
        RegistrationSummaryResponseModel summary = new RegistrationSummaryResponseModel(
                eventId,
                40,
                5,
                100,
                60,
                40.0
        );

        when(eventService.getRegistrationSummary(eventId)).thenReturn(summary);

        // act
        RegistrationSummaryResponseModel result = eventController.getRegistrationSummary(eventId);

        // assert
        assertNotNull(result);
        assertEquals(eventId, result.getEventId());
        assertEquals(40, result.getTotalRegistrations());
        assertEquals(5, result.getWaitlistedCount());
        assertEquals(100, result.getMaxCapacity());
        assertEquals(60, result.getRemainingSpots());
        assertEquals(40.0, result.getPercentageFull());

        verify(eventService, times(1)).getRegistrationSummary(eventId);
        verifyNoMoreInteractions(eventService);
    }
}

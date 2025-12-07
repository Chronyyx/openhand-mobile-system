package com.mana.openhand_backend.events.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.domainclientlayer.EventResponseModel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventResponseMapperTest {

    @Test
    void toResponseModel_whenEventIsNull_returnsNull() {
        EventResponseModel result = EventResponseMapper.toResponseModel(null);
        assertNull(result);
    }

    @Test
    void toResponseModel_whenEventHasAllFields_mapsAllValues() {
        // arrange
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 21, 0);

        Event event = new Event(
                "Title",
                "Description",
                start,
                end,
                "Location",
                "Address",
                EventStatus.OPEN,
                100,
                10
        );

        // act
        EventResponseModel result = EventResponseMapper.toResponseModel(event);

        // assert
        assertNotNull(result);
        assertEquals("Title", result.getTitle());
        assertEquals("Description", result.getDescription());
        assertEquals(start.toString(), result.getStartDateTime());
        assertEquals(end.toString(), result.getEndDateTime());
        assertEquals("Location", result.getLocationName());
        assertEquals("Address", result.getAddress());
        assertEquals(EventStatus.OPEN.name(), result.getStatus());
        assertEquals(100, result.getMaxCapacity());
        assertEquals(10, result.getCurrentRegistrations());
    }

    @Test
    void toResponseModel_whenDateAndStatusAreNull_mapsNullStrings() {
        // arrange
        Event event = new Event(
                "Title",
                "Description",
                null,
                null,
                "Location",
                "Address",
                null,
                null,
                null
        );

        // act
        EventResponseModel result = EventResponseMapper.toResponseModel(event);

        // assert
        assertNotNull(result);
        assertNull(result.getStartDateTime());
        assertNull(result.getEndDateTime());
        assertNull(result.getStatus());
    }
}

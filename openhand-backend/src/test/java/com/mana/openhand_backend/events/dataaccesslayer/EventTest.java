package com.mana.openhand_backend.events.dataaccesslayer;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void noArgsConstructor_allowsSettingAndGettingAllFields() throws Exception {
        // use reflection because constructor is protected (JPA-style)
        Constructor<Event> ctor = Event.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        Event event = ctor.newInstance();

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 1, 12, 0);

        event.setTitle("Title");
        event.setDescription("Desc");
        event.setStartDateTime(start);
        event.setEndDateTime(end);
        event.setLocationName("Location");
        event.setAddress("123 Street");
        event.setStatus(EventStatus.OPEN);
        event.setMaxCapacity(50);
        event.setCurrentRegistrations(5);

        assertNull(event.getId()); // not setcd
        assertEquals("Title", event.getTitle());
        assertEquals("Desc", event.getDescription());
        assertEquals(start, event.getStartDateTime());
        assertEquals(end, event.getEndDateTime());
        assertEquals("Location", event.getLocationName());
        assertEquals("123 Street", event.getAddress());
        assertEquals(EventStatus.OPEN, event.getStatus());
        assertEquals(50, event.getMaxCapacity());
        assertEquals(5, event.getCurrentRegistrations());
    }

    @Test
    void allArgsConstructor_setsAllFieldsCorrectly() {
        LocalDateTime start = LocalDateTime.of(2025, 2, 2, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 2, 2, 11, 0);

        Event event = new Event(
                "Event title",
                "Some description",
                start,
                end,
                "Community Center",
                "456 Avenue",
                EventStatus.FULL,
                100,
                75,
                "General");

        assertEquals("Event title", event.getTitle());
        assertEquals("Some description", event.getDescription());
        assertEquals(start, event.getStartDateTime());
        assertEquals(end, event.getEndDateTime());
        assertEquals("Community Center", event.getLocationName());
        assertEquals("456 Avenue", event.getAddress());
        assertEquals(EventStatus.FULL, event.getStatus());
        assertEquals(100, event.getMaxCapacity());
        assertEquals(75, event.getCurrentRegistrations());
    }
}

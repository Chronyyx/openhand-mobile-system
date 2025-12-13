package com.mana.openhand_backend.events.presentationlayer.payload;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CreateEventRequestTest {

    @Test
    void getParsedStartDateTime_acceptsSpaceOrT() {
        CreateEventRequest req = new CreateEventRequest();
        req.setStartDateTime("2025-01-31 13:45:00");
        LocalDateTime parsed = req.getParsedStartDateTime();
        assertEquals(LocalDateTime.of(2025, 1, 31, 13, 45, 0), parsed);

        req.setStartDateTime("2025-02-01T09:30");
        LocalDateTime parsed2 = req.getParsedStartDateTime();
        assertEquals(LocalDateTime.of(2025, 2, 1, 9, 30), parsed2);
    }

    @Test
    void getParsedEndDateTime_returnsNullWhenBlank() {
        CreateEventRequest req = new CreateEventRequest();
        req.setStartDateTime("2025-01-01T10:00");
        req.setEndDateTime("");
        assertNull(req.getParsedEndDateTime());
    }

    @Test
    void getParsedStartDateTime_whenMissing_throws() {
        CreateEventRequest req = new CreateEventRequest();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, req::getParsedStartDateTime);
        assertTrue(ex.getMessage().contains("startDateTime is required"));
    }

    @Test
    void getParsedEndDateTime_withInvalidFormat_throws() {
        CreateEventRequest req = new CreateEventRequest();
        req.setStartDateTime("2025-01-01T10:00");
        req.setEndDateTime("invalid-date");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, req::getParsedEndDateTime);
        assertTrue(ex.getMessage().contains("endDateTime must be an ISO local datetime"));
    }
}

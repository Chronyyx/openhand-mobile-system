package com.mana.openhand_backend.registrations.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RegistrationHistoryResponseModelTest {

    @Test
    void eventResponseModel_shouldSetFields() {
        RegistrationHistoryEventResponseModel model = new RegistrationHistoryEventResponseModel();

        model.setEventId(10L);
        model.setTitle("Event");
        model.setStartDateTime("2026-01-01T10:00");
        model.setEndDateTime("2026-01-01T12:00");
        model.setLocation("Hall");

        assertEquals(10L, model.getEventId());
        assertEquals("Event", model.getTitle());
        assertEquals("2026-01-01T10:00", model.getStartDateTime());
        assertEquals("2026-01-01T12:00", model.getEndDateTime());
        assertEquals("Hall", model.getLocation());
    }

    @Test
    void historyResponseModel_shouldSetFields() {
        RegistrationHistoryEventResponseModel event = new RegistrationHistoryEventResponseModel(
                11L,
                "Event",
                "2026-01-02T10:00",
                null,
                "Center"
        );

        RegistrationHistoryResponseModel model = new RegistrationHistoryResponseModel(
                5L,
                "CONFIRMED",
                "2026-01-02T09:00",
                RegistrationTimeCategory.ACTIVE,
                event
        );

        assertEquals(5L, model.getRegistrationId());
        assertEquals("CONFIRMED", model.getStatus());
        assertEquals("2026-01-02T09:00", model.getCreatedAt());
        assertEquals(RegistrationTimeCategory.ACTIVE, model.getTimeCategory());
        assertEquals(event, model.getEvent());
        assertNull(event.getEndDateTime());
    }

    @Test
    void historyResponseModel_settersShouldUpdateFields() {
        RegistrationHistoryResponseModel model = new RegistrationHistoryResponseModel();
        RegistrationHistoryEventResponseModel event = new RegistrationHistoryEventResponseModel();

        model.setRegistrationId(7L);
        model.setStatus("CANCELLED");
        model.setCreatedAt("2026-02-01T09:00");
        model.setTimeCategory(RegistrationTimeCategory.PAST);
        model.setEvent(event);

        assertEquals(7L, model.getRegistrationId());
        assertEquals("CANCELLED", model.getStatus());
        assertEquals("2026-02-01T09:00", model.getCreatedAt());
        assertEquals(RegistrationTimeCategory.PAST, model.getTimeCategory());
        assertEquals(event, model.getEvent());
    }
}

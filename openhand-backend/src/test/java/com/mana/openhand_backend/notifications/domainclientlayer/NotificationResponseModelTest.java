package com.mana.openhand_backend.notifications.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationResponseModelTest {

    @Test
    void constructors_shouldPopulateFields() {
        NotificationResponseModel base = new NotificationResponseModel(
                1L,
                2L,
                "Event Title",
                "TYPE",
                "Text",
                true,
                "2026-01-01T10:00",
                "2026-01-01T11:00"
        );

        assertEquals(1L, base.getId());
        assertEquals(2L, base.getEventId());
        assertEquals("Event Title", base.getEventTitle());
        assertEquals("TYPE", base.getNotificationType());
        assertEquals("Text", base.getTextContent());
        assertTrue(base.isRead());
        assertEquals("2026-01-01T10:00", base.getCreatedAt());
        assertEquals("2026-01-01T11:00", base.getReadAt());
        assertNull(base.getEventStartDateTime());
        assertNull(base.getParticipantName());

        NotificationResponseModel withStart = new NotificationResponseModel(
                3L,
                4L,
                "Event Title 2",
                "TYPE_2",
                "Text 2",
                false,
                "2026-02-01T10:00",
                null,
                "2026-02-02T12:00"
        );

        assertEquals("2026-02-02T12:00", withStart.getEventStartDateTime());
        assertNull(withStart.getParticipantName());
        assertFalse(withStart.isRead());

        NotificationResponseModel full = new NotificationResponseModel(
                5L,
                6L,
                "Event Title 3",
                "TYPE_3",
                "Text 3",
                true,
                "2026-03-01T10:00",
                "2026-03-01T11:00",
                "2026-03-02T12:00",
                "Participant"
        );

        assertEquals("2026-03-02T12:00", full.getEventStartDateTime());
        assertEquals("Participant", full.getParticipantName());
    }

    @Test
    void setters_shouldUpdateFields() {
        NotificationResponseModel model = new NotificationResponseModel();

        model.setId(10L);
        model.setEventId(20L);
        model.setEventTitle("Title");
        model.setNotificationType("UPDATE");
        model.setTextContent("Message");
        model.setRead(true);
        model.setCreatedAt("2026-04-01T09:00");
        model.setReadAt("2026-04-01T10:00");
        model.setEventStartDateTime("2026-04-02T11:00");
        model.setParticipantName("Tester");

        assertEquals(10L, model.getId());
        assertEquals(20L, model.getEventId());
        assertEquals("Title", model.getEventTitle());
        assertEquals("UPDATE", model.getNotificationType());
        assertEquals("Message", model.getTextContent());
        assertTrue(model.isRead());
        assertEquals("2026-04-01T09:00", model.getCreatedAt());
        assertEquals("2026-04-01T10:00", model.getReadAt());
        assertEquals("2026-04-02T11:00", model.getEventStartDateTime());
        assertEquals("Tester", model.getParticipantName());
    }
}

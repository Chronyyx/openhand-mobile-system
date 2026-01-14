package com.mana.openhand_backend.notifications.dataaccesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTest {

    @Test
    void setters_shouldUpdateFields() {
        Notification notification = new Notification();

        User user = new User();
        user.setEmail("user@example.com");

        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.parse("2026-01-01T10:00"),
                null,
                "Location",
                "Address",
                EventStatus.OPEN,
                10,
                0,
                "General"
        );

        LocalDateTime createdAt = LocalDateTime.parse("2026-01-01T09:00");
        LocalDateTime readAt = LocalDateTime.parse("2026-01-01T12:00");

        notification.setId(99L);
        notification.setUser(user);
        notification.setEvent(event);
        notification.setNotificationType(NotificationType.REGISTRATION_CONFIRMATION);
        notification.setLanguage("fr");
        notification.setRead(true);
        notification.setCreatedAt(createdAt);
        notification.setReadAt(readAt);
        notification.setTextContent("Hello");
        notification.setEventTitle("Event Title");
        notification.setParticipantName("Participant");

        assertEquals(99L, notification.getId());
        assertEquals(user, notification.getUser());
        assertEquals(event, notification.getEvent());
        assertEquals(NotificationType.REGISTRATION_CONFIRMATION, notification.getNotificationType());
        assertEquals("fr", notification.getLanguage());
        assertTrue(notification.isRead());
        assertEquals(createdAt, notification.getCreatedAt());
        assertEquals(readAt, notification.getReadAt());
        assertEquals("Hello", notification.getTextContent());
        assertEquals("Event Title", notification.getEventTitle());
        assertEquals("Participant", notification.getParticipantName());
    }

    @Test
    void constructor_shouldDefaultReadFalse() {
        User user = new User();
        Event event = new Event(
                "Event",
                "Desc",
                LocalDateTime.parse("2026-01-01T10:00"),
                null,
                "Location",
                "Address",
                EventStatus.OPEN,
                10,
                0,
                "General"
        );

        Notification notification = new Notification(
                user,
                event,
                NotificationType.CANCELLATION,
                "en",
                "Text",
                "Event Title"
        );

        assertFalse(notification.isRead());
        assertEquals("Event Title", notification.getEventTitle());
    }
}

package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationResponseModel;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationResponseMapperTest {

    @Test
    void toResponseModel_mapsNullableFields() {
        Event event = new Event("Title", "Desc", LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2025, 1, 1, 10, 0), "Loc", "Addr",
                EventStatus.OPEN, 10, 0, "CATEGORY");
        ReflectionTestUtils.setField(event, "id", 7L);

        Notification notification = new Notification(null, event, NotificationType.REMINDER,
                "en", "Text", "Event Title");
        ReflectionTestUtils.setField(notification, "id", 3L);
        notification.setReadAt(null);

        NotificationResponseModel response = NotificationResponseMapper.toResponseModel(notification);
        assertEquals(3L, response.getId());
        assertEquals(7L, response.getEventId());
        assertEquals("Event Title", response.getEventTitle());
        assertNull(response.getReadAt());
        assertNotNull(response.getEventStartDateTime());
    }

    @Test
    void toResponseModel_handlesNullEventStartDate() {
        Event event = new Event("Title", "Desc", null, null,
                "Loc", "Addr", EventStatus.OPEN, 10, 0, "CATEGORY");
        ReflectionTestUtils.setField(event, "id", 8L);

        Notification notification = new Notification(null, event, NotificationType.CANCELLATION,
                "en", "Text", "Event Title");
        ReflectionTestUtils.setField(notification, "id", 4L);
        notification.setReadAt(LocalDateTime.of(2025, 1, 2, 12, 0));

        NotificationResponseModel response = NotificationResponseMapper.toResponseModel(notification);
        assertEquals(4L, response.getId());
        assertNotNull(response.getReadAt());
        assertNull(response.getEventStartDateTime());
    }

    @Test
    void defaultConstructor_isCallable() {
        assertNotNull(new NotificationResponseMapper());
    }
}

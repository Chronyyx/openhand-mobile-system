package com.mana.openhand_backend.registrations.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryEventResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationHistoryResponseModel;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationTimeCategory;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationHistoryResponseMapperTest {

    @Test
    void toResponseModel_returnsNullWhenRegistrationNull() {
        assertNull(RegistrationHistoryResponseMapper.toResponseModel(null, RegistrationTimeCategory.ACTIVE));
    }

    @Test
    void toResponseModel_mapsEventFieldsWhenPresent() {
        Event event = new Event(
                "Title",
                "Desc",
                LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2025, 1, 1, 10, 0),
                "Location",
                "Address",
                EventStatus.OPEN,
                10,
                0,
                "CATEGORY"
        );
        ReflectionTestUtils.setField(event, "id", 88L);

        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        Registration registration = new Registration(user, event, RegistrationStatus.CONFIRMED,
                LocalDateTime.of(2024, 12, 1, 8, 30));
        ReflectionTestUtils.setField(registration, "id", 42L);

        RegistrationHistoryResponseModel model = RegistrationHistoryResponseMapper.toResponseModel(
                registration,
                RegistrationTimeCategory.ACTIVE
        );

        assertEquals(42L, model.getRegistrationId());
        assertEquals("CONFIRMED", model.getStatus());
        assertEquals("2024-12-01T08:30", model.getCreatedAt());
        assertEquals(RegistrationTimeCategory.ACTIVE, model.getTimeCategory());

        RegistrationHistoryEventResponseModel eventModel = model.getEvent();
        assertNotNull(eventModel);
        assertEquals(88L, eventModel.getEventId());
        assertEquals("Title", eventModel.getTitle());
        assertEquals("2025-01-01T09:00", eventModel.getStartDateTime());
        assertEquals("2025-01-01T10:00", eventModel.getEndDateTime());
        assertEquals("Location", eventModel.getLocation());
    }

    @Test
    void toResponseModel_handlesMissingEventAndFields() {
        User user = new User("user@example.com", "pwd", Set.of("ROLE_MEMBER"));
        Registration registration = new Registration(user, null, RegistrationStatus.CONFIRMED,
                LocalDateTime.of(2024, 12, 1, 8, 30));
        registration.setStatus(null);
        registration.setRequestedAt(null);

        RegistrationHistoryResponseModel model = RegistrationHistoryResponseMapper.toResponseModel(
                registration,
                RegistrationTimeCategory.PAST
        );

        assertNull(model.getStatus());
        assertNull(model.getCreatedAt());
        assertNull(model.getEvent());
    }
}

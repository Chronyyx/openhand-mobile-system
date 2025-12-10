package com.mana.openhand_backend.registrations.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationResponseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationResponseMapperTest {

    private User testUser;
    private Event testEvent;
    private Registration testRegistration;
    private LocalDateTime now;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        now = LocalDateTime.now();

        testUser = new User();
        testUser.setEmail("test@example.com");
        Field userIdField = User.class.getDeclaredField("id");
        userIdField.setAccessible(true);
        userIdField.set(testUser, 1L);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                now,
                now.plusHours(3),
                "Test Location",
                "123 Test St",
                EventStatus.OPEN,
                100,
                50,
                "General");
        Field eventIdField = Event.class.getDeclaredField("id");
        eventIdField.setAccessible(true);
        eventIdField.set(testEvent, 10L);

        testRegistration = new Registration(testUser, testEvent, RegistrationStatus.CONFIRMED, now);
        Field regIdField = Registration.class.getDeclaredField("id");
        regIdField.setAccessible(true);
        regIdField.set(testRegistration, 100L);
    }

    @Test
    void toResponseModel_withConfirmedRegistration_shouldMapAllFields()
            throws NoSuchFieldException, IllegalAccessException {
        // Arrange
        testRegistration.setConfirmedAt(now.plusHours(1));

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(10L, response.getEventId());
        assertEquals("Test Event", response.getEventTitle());
        assertEquals("CONFIRMED", response.getStatus());
        assertEquals(now.toString(), response.getRequestedAt());
        assertEquals(now.plusHours(1).toString(), response.getConfirmedAt());
        assertNull(response.getCancelledAt());
        assertNull(response.getWaitlistedPosition());
    }

    @Test
    void toResponseModel_withCancelledRegistration_shouldMapCancelledFields() {
        // Arrange
        testRegistration.setStatus(RegistrationStatus.CANCELLED);
        testRegistration.setCancelledAt(now.plusHours(2));

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
        assertEquals(now.plusHours(2).toString(), response.getCancelledAt());
        assertNull(response.getConfirmedAt());
    }

    @Test
    void toResponseModel_withNullRegistration_shouldReturnNull() {
        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(null);

        // Assert
        assertNull(response);
    }

    @Test
    void toResponseModel_withNullRequestedAt_shouldMapToNull() {
        // Arrange
        testRegistration.setRequestedAt(null);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertNull(response.getRequestedAt());
    }

    @Test
    void toResponseModel_withNullConfirmedAt_shouldMapToNull() {
        // Arrange
        testRegistration.setConfirmedAt(null);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertNull(response.getConfirmedAt());
    }

    @Test
    void toResponseModel_withNullCancelledAt_shouldMapToNull() {
        // Arrange
        testRegistration.setCancelledAt(null);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertNull(response.getCancelledAt());
    }

    @Test
    void toResponseModel_withNullStatus_shouldMapToNull() {
        // Arrange
        testRegistration.setStatus(null);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertNull(response.getStatus());
    }

    @Test
    void toResponseModel_withNullEventTitle_shouldMapToNull() {
        // Arrange
        testEvent.setTitle(null);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertNull(response.getEventTitle());
    }

    @Test
    void toResponseModel_withWaitlistedPosition_shouldMapCorrectly() {
        // Arrange
        testRegistration.setWaitlistedPosition(5);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getWaitlistedPosition());
    }

    @Test
    void toResponseModel_withNullWaitlistedPosition_shouldMapToNull() {
        // Arrange
        testRegistration.setWaitlistedPosition(null);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertNull(response.getWaitlistedPosition());
    }

    @Test
    void toResponseModel_withZeroWaitlistedPosition_shouldMapCorrectly() {
        // Arrange
        testRegistration.setWaitlistedPosition(0);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getWaitlistedPosition());
    }

    @Test
    void toResponseModel_multipleRegistrations_shouldMapIndependently()
            throws NoSuchFieldException, IllegalAccessException {
        // Arrange
        Registration registration2 = new Registration(testUser, testEvent);
        registration2.setStatus(RegistrationStatus.CANCELLED);
        registration2.setCancelledAt(now.plusDays(1));
        Field regIdField = Registration.class.getDeclaredField("id");
        regIdField.setAccessible(true);
        regIdField.set(registration2, 101L);

        // Act
        RegistrationResponseModel response1 = RegistrationResponseMapper.toResponseModel(testRegistration);
        RegistrationResponseModel response2 = RegistrationResponseMapper.toResponseModel(registration2);

        // Assert
        assertNotEquals(response1.getId(), response2.getId());
        assertNotEquals(response1.getStatus(), response2.getStatus());
        assertNull(response1.getCancelledAt());
        assertNotNull(response2.getCancelledAt());
    }

    @Test
    void toResponseModel_withAllDatesSet_shouldMapAllDates() {
        // Arrange
        LocalDateTime requestedAt = now;
        LocalDateTime confirmedAt = now.plusHours(1);
        LocalDateTime cancelledAt = now.plusDays(1);

        testRegistration.setRequestedAt(requestedAt);
        testRegistration.setConfirmedAt(confirmedAt);
        testRegistration.setCancelledAt(cancelledAt);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertEquals(requestedAt.toString(), response.getRequestedAt());
        assertEquals(confirmedAt.toString(), response.getConfirmedAt());
        assertEquals(cancelledAt.toString(), response.getCancelledAt());
    }

    @Test
    void toResponseModel_preservesLocalDateTimeFormat() {
        // Arrange
        LocalDateTime specificTime = LocalDateTime.of(2024, 3, 15, 14, 30, 45);
        testRegistration.setRequestedAt(specificTime);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertEquals(specificTime.toString(), response.getRequestedAt());
    }

    @Test
    void toResponseModel_withDifferentEventTitles_shouldMapCorrectly() {
        // Arrange
        String[] titles = { "Gala 2025", "Workshop", "Seminar", "Conference" };

        // Act & Assert
        for (String title : titles) {
            testEvent.setTitle(title);
            RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);
            assertEquals(title, response.getEventTitle());
        }
    }

    @Test
    void toResponseModel_withAllStatusTypes_shouldMapCorrectly() {
        // Arrange
        RegistrationStatus[] statuses = { RegistrationStatus.CONFIRMED, RegistrationStatus.CANCELLED };

        // Act & Assert
        for (RegistrationStatus status : statuses) {
            testRegistration.setStatus(status);
            RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);
            assertEquals(status.name(), response.getStatus());
        }
    }

    @Test
    void toResponseModel_withLargeWaitlistedPosition_shouldMapCorrectly() {
        // Arrange
        testRegistration.setWaitlistedPosition(999);

        // Act
        RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertEquals(999, response.getWaitlistedPosition());
    }

    @Test
    void toResponseModel_callMultipleTimes_shouldReturnDifferentInstances() {
        // Act
        RegistrationResponseModel response1 = RegistrationResponseMapper.toResponseModel(testRegistration);
        RegistrationResponseModel response2 = RegistrationResponseMapper.toResponseModel(testRegistration);

        // Assert
        assertNotSame(response1, response2);
        assertEquals(response1.getId(), response2.getId());
    }

    @Test
    void toResponseModel_withEventNullTitle_shouldNotThrow() {
        // Arrange
        testEvent.setTitle(null);

        // Act & Assert
        assertDoesNotThrow(() -> {
            RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);
            assertNull(response.getEventTitle());
        });
    }

    @Test
    void toResponseModel_withMultipleDateCombinations_shouldMapCorrectly() {
        // Arrange
        LocalDateTime[] dateTimes = {
                LocalDateTime.of(2024, 1, 1, 0, 0, 0),
                LocalDateTime.of(2024, 12, 31, 23, 59, 59),
                LocalDateTime.of(2025, 6, 15, 12, 30, 0)
        };

        // Act & Assert
        for (LocalDateTime dateTime : dateTimes) {
            testRegistration.setRequestedAt(dateTime);
            RegistrationResponseModel response = RegistrationResponseMapper.toResponseModel(testRegistration);
            assertEquals(dateTime.toString(), response.getRequestedAt());
        }
    }
}

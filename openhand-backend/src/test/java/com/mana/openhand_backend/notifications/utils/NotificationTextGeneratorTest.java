package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTextGeneratorTest {

    private NotificationTextGenerator textGenerator;

    @BeforeEach
    void setUp() {
        textGenerator = new NotificationTextGenerator();
    }

    @Test
    void generateText_confirmationInEnglish_returnsEnglishConfirmationText() {
        // Arrange
        String eventTitle = "Annual Gala";
        String language = "en";

        // Act
        String result = textGenerator.generateText(
                NotificationType.REGISTRATION_CONFIRMATION,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("confirmed"));
        assertTrue(result.contains("Thank you for registering"));
    }

    @Test
    void generateText_confirmationInFrench_returnsFrenchConfirmationText() {
        // Arrange
        String eventTitle = "Gala Annuel";
        String language = "fr";

        // Act
        String result = textGenerator.generateText(
                NotificationType.REGISTRATION_CONFIRMATION,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("confirmé"));
        assertTrue(result.contains("Merci de votre inscription"));
    }

    @Test
    void generateText_confirmationInSpanish_returnsSpanishConfirmationText() {
        // Arrange
        String eventTitle = "Gala Anual";
        String language = "es";

        // Act
        String result = textGenerator.generateText(
                NotificationType.REGISTRATION_CONFIRMATION,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("confirmado"));
        assertTrue(result.contains("Gracias por su registro"));
    }

    @Test
    void generateText_confirmationDefaultLanguage_returnsEnglishText() {
        // Arrange
        String eventTitle = "Test Event";
        String language = "unknown";

        // Act
        String result = textGenerator.generateText(
                NotificationType.REGISTRATION_CONFIRMATION,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("confirmed"));
        assertTrue(result.contains("Thank you for registering"));
    }

    @Test
    void generateText_cancellationInEnglish_returnsEnglishCancellationText() {
        // Arrange
        String eventTitle = "Workshop";
        String language = "en";

        // Act
        String result = textGenerator.generateText(
                NotificationType.CANCELLATION,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("cancelled"));
        assertTrue(result.contains("registration"));
    }

    @Test
    void generateText_cancellationInFrench_returnsFrenchCancellationText() {
        // Arrange
        String eventTitle = "Atelier";
        String language = "fr";

        // Act
        String result = textGenerator.generateText(
                NotificationType.CANCELLATION,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("annulée"));
        assertTrue(result.contains("inscription"));
    }

    @Test
    void generateText_cancellationInSpanish_returnsSpanishCancellationText() {
        // Arrange
        String eventTitle = "Taller";
        String language = "es";

        // Act
        String result = textGenerator.generateText(
                NotificationType.CANCELLATION,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("cancelado"));
        assertTrue(result.contains("registro"));
    }

    @Test
    void generateText_reminderInEnglish_includesDateTimeAndEventTitle() {
        // Arrange
        String eventTitle = "Conference";
        String language = "en";
        LocalDateTime eventStartDateTime = LocalDateTime.of(2025, 6, 15, 14, 30);

        // Act
        String result = textGenerator.generateText(
                NotificationType.REMINDER,
                eventTitle,
                language,
                eventStartDateTime
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("Reminder"));
        assertTrue(result.contains("starts"));
        assertTrue(result.contains("Don't forget to attend"));
        assertTrue(result.contains("Jun"));
        assertTrue(result.contains("2025"));
    }

    @Test
    void generateText_reminderInFrench_includesDateTimeAndEventTitle() {
        // Arrange
        String eventTitle = "Conférence";
        String language = "fr";
        LocalDateTime eventStartDateTime = LocalDateTime.of(2025, 6, 15, 14, 30);

        // Act
        String result = textGenerator.generateText(
                NotificationType.REMINDER,
                eventTitle,
                language,
                eventStartDateTime
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("Rappel"));
        assertTrue(result.contains("commence"));
        assertTrue(result.contains("N'oubliez pas"));
    }

    @Test
    void generateText_reminderInSpanish_includesDateTimeAndEventTitle() {
        // Arrange
        String eventTitle = "Conferencia";
        String language = "es";
        LocalDateTime eventStartDateTime = LocalDateTime.of(2025, 6, 15, 14, 30);

        // Act
        String result = textGenerator.generateText(
                NotificationType.REMINDER,
                eventTitle,
                language,
                eventStartDateTime
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("Recordatorio"));
        assertTrue(result.contains("comienza"));
        assertTrue(result.contains("No olvide asistir"));
    }

    @Test
    void generateText_reminderWithNullDateTime_usesTBD() {
        // Arrange
        String eventTitle = "TBD Event";
        String language = "en";

        // Act
        String result = textGenerator.generateText(
                NotificationType.REMINDER,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("TBD"));
    }

    @Test
    void generateText_eventUpdate_withNullDateTime_usesTBD() {
        String eventTitle = "Updated Event";
        String result = textGenerator.generateText(
                NotificationType.EVENT_UPDATE,
                eventTitle,
                "en",
                null
        );

        assertTrue(result.contains(eventTitle));
        assertTrue(result.contains("TBD"));
    }

    @Test
    void generateText_employeeRegistered_defaultNameWhenMissing() {
        String eventTitle = "Staff Event";
        String result = textGenerator.generateText(
                NotificationType.EMPLOYEE_REGISTERED_PARTICIPANT,
                eventTitle,
                "en",
                null,
                null
        );

        assertTrue(result.contains("participant"));
        assertTrue(result.contains(eventTitle));
    }

    @Test
    void generateText_allNotificationTypes_generateUniqueText() {
        // Arrange
        String eventTitle = "Test Event";
        String language = "en";
        LocalDateTime dateTime = LocalDateTime.now().plusDays(1);

        // Act
        String confirmation = textGenerator.generateText(NotificationType.REGISTRATION_CONFIRMATION, eventTitle, language, dateTime);
        String cancellation = textGenerator.generateText(NotificationType.CANCELLATION, eventTitle, language, dateTime);
        String reminder = textGenerator.generateText(NotificationType.REMINDER, eventTitle, language, dateTime);

        // Assert
        assertNotEquals(confirmation, cancellation);
        assertNotEquals(confirmation, reminder);
        assertNotEquals(cancellation, reminder);
    }

    @Test
    void generateText_specialCharactersInEventTitle_handlesCorrectly() {
        // Arrange
        String eventTitle = "Event: \"Special\" & <Important>";
        String language = "en";

        // Act
        String result = textGenerator.generateText(
                NotificationType.REGISTRATION_CONFIRMATION,
                eventTitle,
                language,
                null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(eventTitle));
    }

    private static void assertNotEquals(String unexpected, String actual) {
        if (unexpected.equals(actual)) {
            throw new AssertionError("Values should not be equal: " + unexpected);
        }
    }
}

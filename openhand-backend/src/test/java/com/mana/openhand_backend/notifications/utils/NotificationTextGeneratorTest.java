package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTextGeneratorTest {

    private final NotificationTextGenerator generator = new NotificationTextGenerator();

    @Test
    void generateText_handlesAllTypesAndLanguages() {
        LocalDateTime time = LocalDateTime.of(2025, 1, 1, 10, 30);

        assertTrue(generator.generateText(NotificationType.REGISTRATION_CONFIRMATION, "Gala", "en", time)
                .contains("confirmed"));
        assertTrue(generator.generateText(NotificationType.CANCELLATION, "Gala", "fr", time)
                .contains("annul"));
        assertTrue(generator.generateText(NotificationType.REMINDER, "Gala", "es", time)
                .contains("Recordatorio"));
        assertTrue(generator.generateText(NotificationType.EMPLOYEE_REGISTERED_PARTICIPANT, "Gala", "en", time, "Sam")
                .contains("Sam"));
        assertTrue(generator.generateText(NotificationType.EVENT_UPDATE, "Gala", "fr", time)
                .contains("Mise"));
        assertTrue(generator.generateText(NotificationType.EVENT_CAPACITY_WARNING, "Gala", "en", time)
                .contains("Warning"));
        assertTrue(generator.generateText(NotificationType.EVENT_FULL_ALERT, "Gala", "es", time)
                .contains("Alerta"));
    }

    @Test
    void generateText_handlesNullDateAndParticipant() {
        String reminder = generator.generateText(NotificationType.REMINDER, "Gala", "en", null);
        assertTrue(reminder.contains("TBD"));

        String update = generator.generateText(NotificationType.EVENT_UPDATE, "Gala", "en", null);
        assertTrue(update.contains("TBD"));

        String employee = generator.generateText(NotificationType.EMPLOYEE_REGISTERED_PARTICIPANT, "Gala", "en", null, null);
        assertTrue(employee.contains("participant"));
    }
}

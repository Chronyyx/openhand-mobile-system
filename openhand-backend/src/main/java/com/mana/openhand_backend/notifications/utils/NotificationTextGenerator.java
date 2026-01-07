package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates multilingual notification text based on notification type, event details, and language.
 * This utility provides templates for all notification types in English, French, and Spanish.
 */
@Component
public class NotificationTextGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Generate notification text in the specified language
     * 
     * @param type the notification type
     * @param eventTitle the event title
     * @param language the language code (en, fr, es)
     * @param eventStartDateTime the event start date/time (for reminders)
     * @return the generated notification text
     */
    public String generateText(NotificationType type, String eventTitle, String language, LocalDateTime eventStartDateTime) {
        return switch (type) {
            case REGISTRATION_CONFIRMATION -> generateConfirmationText(eventTitle, language);
            case CANCELLATION -> generateCancellationText(eventTitle, language);
            case REMINDER -> generateReminderText(eventTitle, eventStartDateTime, language);
        };
    }

    private String generateConfirmationText(String eventTitle, String language) {
        return switch (language) {
            case "fr" -> String.format("Vous êtes confirmé(e) pour l'événement : %s. Merci de votre inscription !", eventTitle);
            case "es" -> String.format("Está confirmado para el evento: %s. ¡Gracias por su registro!", eventTitle);
            default -> String.format("You are confirmed for the event: %s. Thank you for registering!", eventTitle);
        };
    }

    private String generateCancellationText(String eventTitle, String language) {
        return switch (language) {
            case "fr" -> String.format("Votre inscription à l'événement %s a été annulée.", eventTitle);
            case "es" -> String.format("Su registro para el evento %s ha sido cancelado.", eventTitle);
            default -> String.format("Your registration for the event %s has been cancelled.", eventTitle);
        };
    }

    private String generateReminderText(String eventTitle, LocalDateTime eventStartDateTime, String language) {
        String formattedDateTime = eventStartDateTime != null 
            ? eventStartDateTime.format(DATE_TIME_FORMATTER) 
            : "TBD";
        
        return switch (language) {
            case "fr" -> String.format("Rappel : L'événement %s commence le %s. N'oubliez pas de vous présenter !", eventTitle, formattedDateTime);
            case "es" -> String.format("Recordatorio: El evento %s comienza el %s. ¡No olvide asistir!", eventTitle, formattedDateTime);
            default -> String.format("Reminder: The event %s starts on %s. Don't forget to attend!", eventTitle, formattedDateTime);
        };
    }
}

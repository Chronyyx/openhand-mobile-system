package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generates multilingual notification text based on notification type, event
 * details, and language.
 * This utility provides templates for all notification types in English,
 * French, and Spanish.
 */
@Component
public class NotificationTextGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    /**
     * Generate notification text in the specified language
     * 
     * @param type               the notification type
     * @param eventTitle         the event title
     * @param language           the language code (en, fr, es)
     * @param eventStartDateTime the event start date/time (for reminders)
     * @return the generated notification text
     */
    public String generateText(NotificationType type, String eventTitle, String language,
            LocalDateTime eventStartDateTime) {
        return generateText(type, eventTitle, language, eventStartDateTime, null);
    }

    /**
     * Generate notification text in the specified language
     * 
     * @param type               the notification type
     * @param eventTitle         the event title
     * @param language           the language code (en, fr, es)
     * @param eventStartDateTime the event start date/time (for reminders)
     * @param participantName    the name of the participant (for
     *                           employee-registered notifications)
     * @return the generated notification text
     */
    public String generateText(NotificationType type, String eventTitle, String language,
            LocalDateTime eventStartDateTime, String participantName) {
        return switch (type) {
            case REGISTRATION_CONFIRMATION -> generateConfirmationText(eventTitle, language);
            case CANCELLATION -> generateCancellationText(eventTitle, language);
            case REMINDER -> generateReminderText(eventTitle, eventStartDateTime, language);
            case EMPLOYEE_REGISTERED_PARTICIPANT ->
                generateEmployeeRegisteredText(eventTitle, participantName, language);
            case EVENT_UPDATE -> generateEventUpdateText(eventTitle, eventStartDateTime, language);
            case EVENT_CAPACITY_WARNING -> generateCapacityWarningText(eventTitle, language);
            case EVENT_FULL_ALERT -> generateEventFullText(eventTitle, language);
            case DONATION_CONFIRMATION -> generateDonationConfirmationText(language);
        };
    }

    private String generateEventUpdateText(String eventTitle, LocalDateTime eventStartDateTime, String language) {
        String formattedDateTime = eventStartDateTime != null
                ? eventStartDateTime.format(DATE_TIME_FORMATTER)
                : "TBD";

        return switch (language) {
            case "fr" -> String.format("Mise à jour : L'événement %s a été modifié. Nouvelle heure : %s.", eventTitle,
                    formattedDateTime);
            case "es" -> String.format("Actualización: El evento %s ha sido modificado. Nueva hora: %s.", eventTitle,
                    formattedDateTime);
            default ->
                String.format("Update: The event %s has been updated. New time: %s.", eventTitle, formattedDateTime);
        };
    }

    private String generateConfirmationText(String eventTitle, String language) {
        return switch (language) {
            case "fr" ->
                String.format("Vous êtes confirmé(e) pour l'événement : %s. Merci de votre inscription !", eventTitle);
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
            case "fr" -> String.format("Rappel : L'événement %s commence le %s. N'oubliez pas de vous présenter !",
                    eventTitle, formattedDateTime);
            case "es" -> String.format("Recordatorio: El evento %s comienza el %s. ¡No olvide asistir!", eventTitle,
                    formattedDateTime);
            default -> String.format("Reminder: The event %s starts on %s. Don't forget to attend!", eventTitle,
                    formattedDateTime);
        };
    }

    private String generateEmployeeRegisteredText(String eventTitle, String participantName, String language) {
        String name = participantName != null ? participantName : "participant";

        return switch (language) {
            case "fr" -> String.format("Vous avez enregistré %s pour l'événement : %s.", name, eventTitle);
            case "es" -> String.format("Ha registrado a %s para el evento: %s.", name, eventTitle);
            default -> String.format("You have registered %s for the event: %s.", name, eventTitle);
        };
    }

    private String generateCapacityWarningText(String eventTitle, String language) {
        return switch (language) {
            case "fr" -> String.format("Attention : L'événement %s est presque complet (80%%).", eventTitle);
            case "es" -> String.format("Advertencia: El evento %s está casi lleno (80%%).", eventTitle);
            default -> String.format("Warning: The event %s is nearly full (80%% capacity).", eventTitle);
        };
    }

    private String generateEventFullText(String eventTitle, String language) {
        return switch (language) {
            case "fr" -> String.format("Alerte : L'événement %s est maintenant complet.", eventTitle);
            case "es" -> String.format("Alerta: El evento %s está completo.", eventTitle);
            default -> String.format("Alert: The event %s is now full.", eventTitle);
        };
    }

    private String generateDonationConfirmationText(String language) {
        return switch (language) {
            case "fr" -> "Merci pour votre don. Votre paiement a été reçu.";
            case "es" -> "Gracias por su donación. Su pago ha sido recibido.";
            default -> "Thank you for your donation. Your payment was received.";
        };
    }
}

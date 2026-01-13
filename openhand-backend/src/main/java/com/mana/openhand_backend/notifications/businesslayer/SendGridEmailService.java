package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.notifications.domainclientlayer.EmailSendResult;
import com.mana.openhand_backend.notifications.utils.SendGridSenderProperties;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Email delivery via SendGrid. This service sits in the business layer to keep controllers thin.
 */
@Service
public class SendGridEmailService {

    private static final Logger logger = LoggerFactory.getLogger(SendGridEmailService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    private final SendGrid sendGrid;
    private final SendGridSenderProperties senderProperties;

    public SendGridEmailService(SendGrid sendGrid, SendGridSenderProperties senderProperties) {
        this.sendGrid = sendGrid;
        this.senderProperties = senderProperties;
    }

    public EmailSendResult sendRegistrationConfirmation(String toEmail, String recipientName, String eventTitle, String language) {
        LocalizedEmailContent content = registrationContent(eventTitle, language);
        return sendEmail(toEmail, recipientName, content.subject(), content.body());
    }

    public EmailSendResult sendReminder(String toEmail, String recipientName, String eventTitle,
                                        LocalDateTime eventStartDateTime, String language) {
        LocalizedEmailContent content = reminderContent(eventTitle, eventStartDateTime, language);
        return sendEmail(toEmail, recipientName, content.subject(), content.body());
    }

    public EmailSendResult sendCancellationOrUpdate(String toEmail, String recipientName, String eventTitle,
                                                    String updateDetails, String language) {
        LocalizedEmailContent content = cancellationOrUpdateContent(eventTitle, updateDetails, language);
        return sendEmail(toEmail, recipientName, content.subject(), content.body());
    }

    public EmailSendResult sendAccountRegistrationConfirmation(String toEmail, String recipientName) {
        LocalizedEmailContent content = accountRegistrationContent(recipientName);
        return sendEmail(toEmail, recipientName, content.subject(), content.body());
    }

    private EmailSendResult sendEmail(String toEmail, String recipientName, String subject, String body) {
        if (toEmail == null || toEmail.isBlank()) {
            String errorMsg = "Recipient email is required";
            logger.error("Failed to send SendGrid email: {}", errorMsg);
            return EmailSendResult.failure(errorMsg);
        }
        if (senderProperties.fromEmail() == null || senderProperties.fromEmail().isBlank()) {
            String errorMsg = "SendGrid from email is not configured";
            logger.error("Failed to send SendGrid email: {}", errorMsg);
            return EmailSendResult.failure(errorMsg);
        }

        try {
            Mail mail = new Mail();
            Email from = senderProperties.fromName() == null || senderProperties.fromName().isBlank()
                    ? new Email(senderProperties.fromEmail())
                    : new Email(senderProperties.fromEmail(), senderProperties.fromName());
            Email to = recipientName == null || recipientName.isBlank()
                    ? new Email(toEmail)
                    : new Email(toEmail, recipientName);

            mail.setFrom(from);
            mail.setSubject(subject);
            mail.addContent(new Content("text/plain", body));

            Personalization personalization = new Personalization();
            personalization.addTo(to);
            mail.addPersonalization(personalization);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                logger.info("SendGrid email sent: subject='{}', to='{}', status={}", subject, toEmail, response.getStatusCode());
                return EmailSendResult.ok();
            }

            String errorMsg = String.format("SendGrid responded with status %d: %s", response.getStatusCode(), response.getBody());
            logger.error("Failed to send SendGrid email to {}. {}", toEmail, errorMsg);
            return EmailSendResult.failure(errorMsg);
        } catch (IOException ex) {
            logger.error("Exception while sending SendGrid email to {}", toEmail, ex);
            return EmailSendResult.failure(ex.getMessage());
        }
    }

    private LocalizedEmailContent registrationContent(String eventTitle, String language) {
        String lang = normalizeLanguage(language);
        return switch (lang) {
            case "fr" -> new LocalizedEmailContent(
                    "Confirmation d'inscription : " + eventTitle,
                    "Vous êtes inscrit à l'événement \"" + eventTitle + "\". Merci de votre inscription."
            );
            case "spa" -> new LocalizedEmailContent(
                    "Confirmación de registro: " + eventTitle,
                    "Está registrado en el evento \"" + eventTitle + "\". Gracias por registrarse."
            );
            default -> new LocalizedEmailContent(
                    "Registration confirmed: " + eventTitle,
                    "You are registered for \"" + eventTitle + "\". Thank you for joining us."
            );
        };
    }

    private LocalizedEmailContent reminderContent(String eventTitle, LocalDateTime startDateTime, String language) {
        String formattedDate = startDateTime == null
                ? "Date to be announced"
                : startDateTime.format(DATE_TIME_FORMATTER.withLocale(Locale.ENGLISH));
        String lang = normalizeLanguage(language);

        return switch (lang) {
            case "fr" -> new LocalizedEmailContent(
                    "Rappel : " + eventTitle,
                    "Rappel pour \"" + eventTitle + "\" le " + formattedDate + ". Nous avons hate de vous voir."
            );
            case "spa" -> new LocalizedEmailContent(
                    "Recordatorio: " + eventTitle,
                    "Recordatorio para \"" + eventTitle + "\" el " + formattedDate + ". Lo esperamos."
            );
            default -> new LocalizedEmailContent(
                    "Reminder: " + eventTitle,
                    "Reminder for \"" + eventTitle + "\" on " + formattedDate + ". We look forward to seeing you."
            );
        };
    }

    private LocalizedEmailContent cancellationOrUpdateContent(String eventTitle, String updateDetails, String language) {
        String lang = normalizeLanguage(language);
        String details = (updateDetails == null || updateDetails.isBlank())
                ? ""
                : " Details: " + updateDetails;

        return switch (lang) {
            case "fr" -> new LocalizedEmailContent(
                    "Mise à jour importante : " + eventTitle,
                    "Votre événement \"" + eventTitle + "\" a change ou est annule." + details
            );
            case "spa" -> new LocalizedEmailContent(
                    "Actualización importante: " + eventTitle,
                    "Su evento \"" + eventTitle + "\" ha cambiado o ha sido cancelado." + details
            );
            default -> new LocalizedEmailContent(
                    "Important update: " + eventTitle,
                    "Your event \"" + eventTitle + "\" has changed or has been cancelled." + details
            );
        };
    }

    private LocalizedEmailContent accountRegistrationContent(String recipientName) {
        String displayName = (recipientName == null || recipientName.isBlank()) ? "there" : recipientName;
        String subject = "Welcome to MANA";
        String body = "Hi " + displayName + ",\n\n"
                + "Your MANA account has been created successfully. You can now log in and register for events.\n\n"
                + "Thanks,\n"
                + "The MANA Team";
        return new LocalizedEmailContent(subject, body);
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "fr" -> "fr";
            case "spa", "es", "es-419" -> "spa";
            default -> "en";
        };
    }

    private record LocalizedEmailContent(String subject, String body) {
    }
}

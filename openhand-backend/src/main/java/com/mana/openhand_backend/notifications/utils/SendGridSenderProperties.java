package com.mana.openhand_backend.notifications.utils;

/**
 * Holder for SendGrid sender metadata resolved from environment variables.
 */
public record SendGridSenderProperties(String fromEmail, String fromName) {
}

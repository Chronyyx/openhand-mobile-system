package com.mana.openhand_backend.notifications.config;

import com.mana.openhand_backend.notifications.utils.SendGridSenderProperties;
import com.sendgrid.SendGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SendGrid client configuration backed by environment variables.
 */
@Configuration
public class SendGridConfig {

    private static final Logger logger = LoggerFactory.getLogger(SendGridConfig.class);

    @Bean
    public SendGrid sendGridClient(@Value("${SENDGRID_API_KEY:}") String apiKey) {
        if (apiKey.isBlank()) {
            logger.warn("SENDGRID_API_KEY is not configured; SendGrid email delivery will fail until it is set.");
        }
        return new SendGrid(apiKey);
    }

    @Bean
    public SendGridSenderProperties sendGridSenderProperties(
            @Value("${SENDGRID_FROM_EMAIL:}") String fromEmail,
            @Value("${SENDGRID_FROM_NAME:}") String fromName) {
        if (fromEmail.isBlank()) {
            logger.warn("SENDGRID_FROM_EMAIL is not configured; SendGrid email delivery will fail until it is set.");
        }
        return new SendGridSenderProperties(fromEmail, fromName);
    }
}

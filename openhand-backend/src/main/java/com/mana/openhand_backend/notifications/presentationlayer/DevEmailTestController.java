package com.mana.openhand_backend.notifications.presentationlayer;

import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.notifications.domainclientlayer.EmailSendResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only endpoint to exercise SendGrid email delivery.
 * TODO: remove once dedicated dev profile wiring is in place.
 */
@RestController
@RequestMapping("/api/dev/email")
public class DevEmailTestController {

    private final SendGridEmailService sendGridEmailService;

    public DevEmailTestController(SendGridEmailService sendGridEmailService) {
        this.sendGridEmailService = sendGridEmailService;
    }

    @PostMapping("/test")
    public EmailSendResult sendTestEmail(
            @RequestParam("to") String toEmail,
            @RequestParam(value = "lang", defaultValue = "en") String language) {

        String sampleRecipientName = "MANA Member";
        String sampleEventTitle = "Community Gathering";

        return sendGridEmailService.sendRegistrationConfirmation(
                toEmail,
                sampleRecipientName,
                sampleEventTitle,
                language
        );
    }
}

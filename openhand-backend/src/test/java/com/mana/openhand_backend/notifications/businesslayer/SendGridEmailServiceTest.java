package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.notifications.domainclientlayer.EmailSendResult;
import com.mana.openhand_backend.notifications.utils.SendGridSenderProperties;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendGridEmailServiceTest {

    @Mock
    private SendGrid sendGrid;

    private SendGridEmailService service;

    @BeforeEach
    void setUp() {
        SendGridSenderProperties props = new SendGridSenderProperties("from@example.com", "MANA");
        service = new SendGridEmailService(sendGrid, props);
    }

    @Test
    void sendRegistrationConfirmation_success() throws Exception {
        Response ok = buildResponse(202, "Accepted");
        when(sendGrid.api(any(Request.class))).thenReturn(ok);

        EmailSendResult result = service.sendRegistrationConfirmation("to@example.com", "User", "Event", "en");

        assertThat(result.success()).isTrue();
        verify(sendGrid, times(1)).api(any(Request.class));
    }

    @Test
    void sendReminder_success() throws Exception {
        Response ok = buildResponse(200, "OK");
        when(sendGrid.api(any(Request.class))).thenReturn(ok);

        EmailSendResult result = service.sendReminder("to@example.com", "User", "Event",
                LocalDateTime.now(), "spa");

        assertThat(result.success()).isTrue();
        verify(sendGrid, times(1)).api(any(Request.class));
    }

    @Test
    void sendCancellationOrUpdate_success() throws Exception {
        Response ok = buildResponse(202, "OK");
        when(sendGrid.api(any(Request.class))).thenReturn(ok);

        EmailSendResult result = service.sendCancellationOrUpdate("to@example.com", "User", "Event",
                "Details", "fr");

        assertThat(result.success()).isTrue();
        verify(sendGrid, times(1)).api(any(Request.class));
    }

    @Test
    void sendEmail_failureWhenNon2xxResponse() throws Exception {
        Response error = buildResponse(500, "SendGrid error");
        when(sendGrid.api(any(Request.class))).thenReturn(error);

        EmailSendResult result = service.sendRegistrationConfirmation("to@example.com", "User", "Event", "en");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("500");
        verify(sendGrid, times(1)).api(any(Request.class));
    }

    @Test
    void sendEmail_failureOnException() throws Exception {
        when(sendGrid.api(any(Request.class))).thenThrow(new IOException("network down"));

        EmailSendResult result = service.sendReminder("to@example.com", "User", "Event",
                LocalDateTime.now(), "en");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("network down");
    }

    @Test
    void sendEmail_failureWhenFromEmailMissing() {
        SendGridEmailService localService = new SendGridEmailService(
                sendGrid,
                new SendGridSenderProperties("", "MANA"));

        EmailSendResult result = localService.sendRegistrationConfirmation("to@example.com", "User", "Event", "en");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("from email");
        verifyNoInteractions(sendGrid);
    }

    @Test
    void sendEmail_failureWhenRecipientMissing() {
        EmailSendResult result = service.sendRegistrationConfirmation("", "User", "Event", "en");

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("Recipient email is required");
        verifyNoInteractions(sendGrid);
    }

    @Test
    void sendEmail_accountRegistrationUsesDefaults() throws Exception {
        Response ok = buildResponse(202, "OK");
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(sendGrid.api(any(Request.class))).thenReturn(ok);

        EmailSendResult result = service.sendAccountRegistrationConfirmation("to@example.com", null);

        assertThat(result.success()).isTrue();
        verify(sendGrid).api(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        assertThat(request.getBody()).contains("Welcome to MANA");
    }

    private Response buildResponse(int status, String body) {
        Response response = new Response();
        response.setStatusCode(status);
        response.setBody(body);
        response.setHeaders(Collections.emptyMap());
        return response;
    }
}

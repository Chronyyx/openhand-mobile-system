package com.mana.openhand_backend.registrations.presentationlayer;

import com.mana.openhand_backend.registrations.utils.AlreadyRegisteredException;
import com.mana.openhand_backend.registrations.utils.EventCapacityException;
import com.mana.openhand_backend.registrations.utils.EventCompletedException;
import com.mana.openhand_backend.registrations.utils.RegistrationNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationExceptionHandlerTest {

    private final RegistrationExceptionHandler handler = new RegistrationExceptionHandler();

    @Test
    void handleEventCapacityException_buildsConflictPayload() {
        EventCapacityException ex = new EventCapacityException(5L);

        ResponseEntity<Map<String, Object>> response = handler.handleEventCapacityException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().get("status"));
        assertEquals("Event Capacity Exceeded", response.getBody().get("error"));
        assertEquals(ex.getMessage(), response.getBody().get("message"));
        assertEquals(5L, response.getBody().get("eventId"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleEventCompletedException_buildsConflictPayload() {
        EventCompletedException ex = new EventCompletedException(7L);

        ResponseEntity<Map<String, Object>> response = handler.handleEventCompletedException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Event Completed", response.getBody().get("error"));
        assertEquals(7L, response.getBody().get("eventId"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleAlreadyRegisteredException_buildsConflictPayload() {
        AlreadyRegisteredException ex = new AlreadyRegisteredException(4L, 6L);

        ResponseEntity<Map<String, Object>> response = handler.handleAlreadyRegisteredException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Already Registered", response.getBody().get("error"));
        assertEquals(ex.getMessage(), response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    void handleRegistrationNotFoundException_buildsNotFoundPayload() {
        RegistrationNotFoundException ex = new RegistrationNotFoundException(3L);

        ResponseEntity<Map<String, Object>> response = handler.handleRegistrationNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Registration Not Found", response.getBody().get("error"));
        assertEquals(ex.getMessage(), response.getBody().get("message"));
        assertNotNull(response.getBody().get("timestamp"));
    }
}

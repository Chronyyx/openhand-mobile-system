package com.mana.openhand_backend.registrations.presentationlayer;

import com.mana.openhand_backend.registrations.utils.AlreadyRegisteredException;
import com.mana.openhand_backend.registrations.utils.EventCapacityException;
import com.mana.openhand_backend.registrations.utils.EventCompletedException;
import com.mana.openhand_backend.registrations.utils.RegistrationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for registration-related HTTP requests.
 * Provides consistent error responses with appropriate HTTP status codes.
 */
@RestControllerAdvice
public class RegistrationExceptionHandler {

    /**
     * Handles EventCapacityException - thrown when an event reaches capacity during registration.
     * This indicates a race condition scenario where the event became full between checks.
     * Returns HTTP 409 Conflict to inform the client that the registration was not successful
     * and to retry or handle appropriately (often placing user on waitlist).
     */
    @ExceptionHandler(EventCapacityException.class)
    public ResponseEntity<Map<String, Object>> handleEventCapacityException(EventCapacityException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.CONFLICT.value());
        errorResponse.put("error", "Event Capacity Exceeded");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("eventId", ex.getEventId());
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(EventCompletedException.class)
    public ResponseEntity<Map<String, Object>> handleEventCompletedException(EventCompletedException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.CONFLICT.value());
        errorResponse.put("error", "Event Completed");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("eventId", ex.getEventId());
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles AlreadyRegisteredException - thrown when a user attempts to register
     * for an event they're already registered for (active registration).
     * Returns HTTP 409 Conflict.
     */
    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyRegisteredException(AlreadyRegisteredException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.CONFLICT.value());
        errorResponse.put("error", "Already Registered");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles RegistrationNotFoundException - thrown when a registration cannot be found.
     * Returns HTTP 404 Not Found.
     */
    @ExceptionHandler(RegistrationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRegistrationNotFoundException(RegistrationNotFoundException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Registration Not Found");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
}

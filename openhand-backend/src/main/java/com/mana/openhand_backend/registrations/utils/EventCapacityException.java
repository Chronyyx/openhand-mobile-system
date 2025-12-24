package com.mana.openhand_backend.registrations.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an event has reached its maximum capacity during a concurrent registration.
 * This typically indicates a race condition where the event became full between the capacity check
 * and the registration confirmation.
 *
 * Returns HTTP 409 Conflict status to the client.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EventCapacityException extends RuntimeException {

    private final Long eventId;

    public EventCapacityException(Long eventId) {
        super("Event " + eventId + " has reached maximum capacity. You have been added to the waitlist.");
        this.eventId = eventId;
    }

    public Long getEventId() {
        return eventId;
    }
}

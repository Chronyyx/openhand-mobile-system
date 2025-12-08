package com.mana.openhand_backend.registrations.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EventFullException extends RuntimeException {

    public EventFullException(Long eventId) {
        super("Event " + eventId + " is full and waitlist is not available");
    }
}

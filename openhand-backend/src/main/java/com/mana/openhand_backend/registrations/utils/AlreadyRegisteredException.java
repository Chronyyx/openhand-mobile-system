package com.mana.openhand_backend.registrations.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyRegisteredException extends RuntimeException {

    public AlreadyRegisteredException(Long userId, Long eventId) {
        super("User " + userId + " is already registered for event " + eventId);
    }
}

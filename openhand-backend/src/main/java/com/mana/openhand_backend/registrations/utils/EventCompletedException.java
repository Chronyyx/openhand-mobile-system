package com.mana.openhand_backend.registrations.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class EventCompletedException extends RuntimeException {

    private final Long eventId;

    public EventCompletedException(Long eventId) {
        super("Event " + eventId + " is completed and no longer accepts registrations.");
        this.eventId = eventId;
    }

    public Long getEventId() {
        return eventId;
    }
}

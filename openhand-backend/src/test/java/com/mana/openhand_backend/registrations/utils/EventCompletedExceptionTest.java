package com.mana.openhand_backend.registrations.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class EventCompletedExceptionTest {

    @Test
    void message_containsEventId() {
        EventCompletedException ex = new EventCompletedException(9L);

        assertTrue(ex.getMessage().contains("9"));
        assertEquals(9L, ex.getEventId());
    }

    @Test
    void responseStatus_isConflict() {
        ResponseStatus status = EventCompletedException.class.getAnnotation(ResponseStatus.class);

        assertNotNull(status);
        assertEquals(HttpStatus.CONFLICT, status.value());
    }
}

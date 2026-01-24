package com.mana.openhand_backend.registrations.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class EventCapacityExceptionTest {

    @Test
    void message_containsEventId() {
        EventCapacityException ex = new EventCapacityException(12L);

        assertTrue(ex.getMessage().contains("12"));
        assertEquals(12L, ex.getEventId());
    }

    @Test
    void responseStatus_isConflict() {
        ResponseStatus status = EventCapacityException.class.getAnnotation(ResponseStatus.class);

        assertNotNull(status);
        assertEquals(HttpStatus.CONFLICT, status.value());
    }
}

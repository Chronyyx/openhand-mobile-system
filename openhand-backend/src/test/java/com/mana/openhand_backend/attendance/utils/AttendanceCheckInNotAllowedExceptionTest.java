package com.mana.openhand_backend.attendance.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceCheckInNotAllowedExceptionTest {

    @Test
    void message_containsUserAndEvent() {
        AttendanceCheckInNotAllowedException ex = new AttendanceCheckInNotAllowedException(10L, 20L);

        assertEquals("Check-in not allowed for user 20 at event 10", ex.getMessage());
    }

    @Test
    void responseStatus_isBadRequest() {
        ResponseStatus status = AttendanceCheckInNotAllowedException.class.getAnnotation(ResponseStatus.class);

        assertNotNull(status);
        assertEquals(HttpStatus.BAD_REQUEST, status.value());
    }
}

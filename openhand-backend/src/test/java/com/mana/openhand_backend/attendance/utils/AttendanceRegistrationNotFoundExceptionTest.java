package com.mana.openhand_backend.attendance.utils;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceRegistrationNotFoundExceptionTest {

    @Test
    void message_containsUserAndEvent() {
        AttendanceRegistrationNotFoundException ex = new AttendanceRegistrationNotFoundException(9L, 3L);

        assertEquals("Registration not found for user 3 and event 9", ex.getMessage());
    }

    @Test
    void responseStatus_isNotFound() {
        ResponseStatus status = AttendanceRegistrationNotFoundException.class.getAnnotation(ResponseStatus.class);

        assertNotNull(status);
        assertEquals(HttpStatus.NOT_FOUND, status.value());
    }
}

package com.mana.openhand_backend.attendance.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AttendanceRegistrationNotFoundException extends RuntimeException {

    public AttendanceRegistrationNotFoundException(Long eventId, Long userId) {
        super("Registration not found for user " + userId + " and event " + eventId);
    }
}

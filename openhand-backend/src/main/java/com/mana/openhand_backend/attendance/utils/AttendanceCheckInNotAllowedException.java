package com.mana.openhand_backend.attendance.utils;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AttendanceCheckInNotAllowedException extends RuntimeException {

    public AttendanceCheckInNotAllowedException(Long eventId, Long userId) {
        super("Check-in not allowed for user " + userId + " at event " + eventId);
    }
}

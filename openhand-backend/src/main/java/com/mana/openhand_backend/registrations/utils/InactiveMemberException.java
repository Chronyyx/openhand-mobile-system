package com.mana.openhand_backend.registrations.utils;

public class InactiveMemberException extends RuntimeException {
    public InactiveMemberException(Long userId) {
        super("User with id " + userId + " is inactive and cannot register for new events");
    }

    public InactiveMemberException(String message) {
        super(message);
    }
}

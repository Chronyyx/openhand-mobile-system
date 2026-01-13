package com.mana.openhand_backend.notifications.domainclientlayer;

/**
 * Result of an email send attempt.
 */
public record EmailSendResult(boolean success, String errorMessage) {

    public static EmailSendResult ok() {
        return new EmailSendResult(true, null);
    }

    public static EmailSendResult failure(String message) {
        return new EmailSendResult(false, message);
    }
}

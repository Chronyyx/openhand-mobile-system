package com.mana.openhand_backend.registrations.utils;

public class GroupRegistrationCapacityException extends RuntimeException {
    private final Long eventId;
    private final int requestedParticipants;
    private final int remainingCapacity;

    public GroupRegistrationCapacityException(Long eventId, int requestedParticipants, int remainingCapacity) {
        super("Not enough remaining capacity for " + requestedParticipants + " participants. Remaining capacity: "
                + remainingCapacity + ".");
        this.eventId = eventId;
        this.requestedParticipants = requestedParticipants;
        this.remainingCapacity = remainingCapacity;
    }

    public Long getEventId() {
        return eventId;
    }

    public int getRequestedParticipants() {
        return requestedParticipants;
    }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }
}

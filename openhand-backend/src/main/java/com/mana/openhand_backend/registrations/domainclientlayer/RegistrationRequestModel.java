package com.mana.openhand_backend.registrations.domainclientlayer;

public class RegistrationRequestModel {

    private Long eventId;

    public RegistrationRequestModel() {
    }

    public RegistrationRequestModel(Long eventId) {
        this.eventId = eventId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}

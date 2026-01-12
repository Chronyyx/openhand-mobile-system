package com.mana.openhand_backend.registrations.domainclientlayer;

public class EmployeeRegistrationRequestModel {

    private Long userId;
    private Long eventId;

    public EmployeeRegistrationRequestModel() {}

    public EmployeeRegistrationRequestModel(Long userId, Long eventId) {
        this.userId = userId;
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }
}

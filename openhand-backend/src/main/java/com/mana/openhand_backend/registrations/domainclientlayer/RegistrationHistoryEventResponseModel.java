package com.mana.openhand_backend.registrations.domainclientlayer;

public class RegistrationHistoryEventResponseModel {

    private Long eventId;
    private String title;
    private String startDateTime;
    private String endDateTime;
    private String location;

    public RegistrationHistoryEventResponseModel() {
    }

    public RegistrationHistoryEventResponseModel(Long eventId, String title, String startDateTime,
            String endDateTime, String location) {
        this.eventId = eventId;
        this.title = title;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.location = location;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}

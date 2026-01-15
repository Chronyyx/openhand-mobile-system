package com.mana.openhand_backend.events.domainclientlayer;

import java.util.List;

public class EventAttendeesResponseModel {
    private Long eventId;
    private Integer totalAttendees;
    private List<EventAttendeeResponseModel> attendees;

    public EventAttendeesResponseModel() {
    }

    public EventAttendeesResponseModel(Long eventId, Integer totalAttendees, List<EventAttendeeResponseModel> attendees) {
        this.eventId = eventId;
        this.totalAttendees = totalAttendees;
        this.attendees = attendees;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getTotalAttendees() {
        return totalAttendees;
    }

    public void setTotalAttendees(Integer totalAttendees) {
        this.totalAttendees = totalAttendees;
    }

    public List<EventAttendeeResponseModel> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<EventAttendeeResponseModel> attendees) {
        this.attendees = attendees;
    }
}

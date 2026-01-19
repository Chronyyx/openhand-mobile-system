package com.mana.openhand_backend.attendance.domainclientlayer;

import java.util.List;

public class AttendanceEventAttendeesResponseModel {
    private Long eventId;
    private Integer registeredCount;
    private Integer checkedInCount;
    private List<AttendanceAttendeeResponseModel> attendees;

    public AttendanceEventAttendeesResponseModel() {
    }

    public AttendanceEventAttendeesResponseModel(
            Long eventId,
            Integer registeredCount,
            Integer checkedInCount,
            List<AttendanceAttendeeResponseModel> attendees) {
        this.eventId = eventId;
        this.registeredCount = registeredCount;
        this.checkedInCount = checkedInCount;
        this.attendees = attendees;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(Integer registeredCount) {
        this.registeredCount = registeredCount;
    }

    public Integer getCheckedInCount() {
        return checkedInCount;
    }

    public void setCheckedInCount(Integer checkedInCount) {
        this.checkedInCount = checkedInCount;
    }

    public List<AttendanceAttendeeResponseModel> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<AttendanceAttendeeResponseModel> attendees) {
        this.attendees = attendees;
    }
}

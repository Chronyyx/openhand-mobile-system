package com.mana.openhand_backend.attendance.domainclientlayer;

import java.time.LocalDateTime;

public class AttendanceReportResponseModel {

    private Long eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private Integer totalAttended;
    private Integer totalRegistered;
    private Double attendanceRate;

    public AttendanceReportResponseModel(
            Long eventId,
            String eventTitle,
            LocalDateTime eventDate,
            Integer totalAttended,
            Integer totalRegistered,
            Double attendanceRate) {
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventDate = eventDate;
        this.totalAttended = totalAttended;
        this.totalRegistered = totalRegistered;
        this.attendanceRate = attendanceRate;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public Integer getTotalAttended() {
        return totalAttended;
    }

    public Integer getTotalRegistered() {
        return totalRegistered;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }
}

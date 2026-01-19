package com.mana.openhand_backend.attendance.domainclientlayer;

public class AttendanceUpdateResponseModel {
    private Long eventId;
    private Long userId;
    private boolean checkedIn;
    private String checkedInAt;
    private Integer registeredCount;
    private Integer checkedInCount;
    private Double occupancyPercent;

    public AttendanceUpdateResponseModel() {
    }

    public AttendanceUpdateResponseModel(
            Long eventId,
            Long userId,
            boolean checkedIn,
            String checkedInAt,
            Integer registeredCount,
            Integer checkedInCount,
            Double occupancyPercent) {
        this.eventId = eventId;
        this.userId = userId;
        this.checkedIn = checkedIn;
        this.checkedInAt = checkedInAt;
        this.registeredCount = registeredCount;
        this.checkedInCount = checkedInCount;
        this.occupancyPercent = occupancyPercent;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isCheckedIn() {
        return checkedIn;
    }

    public void setCheckedIn(boolean checkedIn) {
        this.checkedIn = checkedIn;
    }

    public String getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(String checkedInAt) {
        this.checkedInAt = checkedInAt;
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

    public Double getOccupancyPercent() {
        return occupancyPercent;
    }

    public void setOccupancyPercent(Double occupancyPercent) {
        this.occupancyPercent = occupancyPercent;
    }
}

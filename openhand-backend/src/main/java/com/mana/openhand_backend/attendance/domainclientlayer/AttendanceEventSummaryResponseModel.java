package com.mana.openhand_backend.attendance.domainclientlayer;

public class AttendanceEventSummaryResponseModel {
    private Long eventId;
    private String title;
    private String startDateTime;
    private String endDateTime;
    private String locationName;
    private String address;
    private String status;
    private Integer maxCapacity;
    private Integer registeredCount;
    private Integer checkedInCount;
    private Double occupancyPercent;

    public AttendanceEventSummaryResponseModel() {
    }

    public AttendanceEventSummaryResponseModel(
            Long eventId,
            String title,
            String startDateTime,
            String endDateTime,
            String locationName,
            String address,
            String status,
            Integer maxCapacity,
            Integer registeredCount,
            Integer checkedInCount,
            Double occupancyPercent) {
        this.eventId = eventId;
        this.title = title;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.locationName = locationName;
        this.address = address;
        this.status = status;
        this.maxCapacity = maxCapacity;
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

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
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

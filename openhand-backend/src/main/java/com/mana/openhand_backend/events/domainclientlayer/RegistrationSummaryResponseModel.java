package com.mana.openhand_backend.events.domainclientlayer;

public class RegistrationSummaryResponseModel {

    private Long eventId;
    private Integer totalRegistrations;
    private Integer waitlistedCount;
    private Integer maxCapacity;
    private Integer remainingSpots;
    private Double percentageFull;

    public RegistrationSummaryResponseModel() {
    }

    public RegistrationSummaryResponseModel(Long eventId, Integer totalRegistrations, 
                                           Integer waitlistedCount, Integer maxCapacity,
                                           Integer remainingSpots, Double percentageFull) {
        this.eventId = eventId;
        this.totalRegistrations = totalRegistrations;
        this.waitlistedCount = waitlistedCount;
        this.maxCapacity = maxCapacity;
        this.remainingSpots = remainingSpots;
        this.percentageFull = percentageFull;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getTotalRegistrations() {
        return totalRegistrations;
    }

    public void setTotalRegistrations(Integer totalRegistrations) {
        this.totalRegistrations = totalRegistrations;
    }

    public Integer getWaitlistedCount() {
        return waitlistedCount;
    }

    public void setWaitlistedCount(Integer waitlistedCount) {
        this.waitlistedCount = waitlistedCount;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getRemainingSpots() {
        return remainingSpots;
    }

    public void setRemainingSpots(Integer remainingSpots) {
        this.remainingSpots = remainingSpots;
    }

    public Double getPercentageFull() {
        return percentageFull;
    }

    public void setPercentageFull(Double percentageFull) {
        this.percentageFull = percentageFull;
    }
}

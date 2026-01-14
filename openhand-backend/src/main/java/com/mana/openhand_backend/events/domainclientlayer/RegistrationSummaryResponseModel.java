package com.mana.openhand_backend.events.domainclientlayer;

import com.mana.openhand_backend.registrations.domainclientlayer.AttendeeResponseModel;
import java.util.List;

public class RegistrationSummaryResponseModel {

    private Long eventId;
    private Integer totalRegistrations;
    private Integer waitlistedCount;
    private Integer maxCapacity;
    private Integer remainingSpots;
    private Double percentageFull;
    private List<AttendeeResponseModel> attendees;

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

    public RegistrationSummaryResponseModel(Long eventId, Integer totalRegistrations, 
                                           Integer waitlistedCount, Integer maxCapacity,
                                           Integer remainingSpots, Double percentageFull,
                                           List<AttendeeResponseModel> attendees) {
        this.eventId = eventId;
        this.totalRegistrations = totalRegistrations;
        this.waitlistedCount = waitlistedCount;
        this.maxCapacity = maxCapacity;
        this.remainingSpots = remainingSpots;
        this.percentageFull = percentageFull;
        this.attendees = attendees;
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

    public List<AttendeeResponseModel> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<AttendeeResponseModel> attendees) {
        this.attendees = attendees;
    }
}

package com.mana.openhand_backend.registrations.domainclientlayer;

public class RegistrationResponseModel {

    private Long id;
    private Long userId;
    private Long eventId;
    private String eventTitle;
    private String status;
    private String requestedAt;
    private String confirmedAt;
    private String cancelledAt;
    private Integer waitlistedPosition;

    public RegistrationResponseModel() {
    }

    public RegistrationResponseModel(Long id, Long userId, Long eventId, String eventTitle,
                                     String status, String requestedAt, String confirmedAt,
                                     String cancelledAt, Integer waitlistedPosition) {
        this.id = id;
        this.userId = userId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.status = status;
        this.requestedAt = requestedAt;
        this.confirmedAt = confirmedAt;
        this.cancelledAt = cancelledAt;
        this.waitlistedPosition = waitlistedPosition;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(String confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(String cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Integer getWaitlistedPosition() {
        return waitlistedPosition;
    }

    public void setWaitlistedPosition(Integer waitlistedPosition) {
        this.waitlistedPosition = waitlistedPosition;
    }
}

package com.mana.openhand_backend.registrations.domainclientlayer;

public class RegistrationHistoryResponseModel {

    private Long registrationId;
    private String status;
    private String createdAt;
    private RegistrationTimeCategory timeCategory;
    private RegistrationHistoryEventResponseModel event;
    private java.util.List<ParticipantResponseModel> participants;

    public RegistrationHistoryResponseModel() {
    }

    public RegistrationHistoryResponseModel(Long registrationId, String status, String createdAt,
            RegistrationTimeCategory timeCategory, RegistrationHistoryEventResponseModel event,
            java.util.List<ParticipantResponseModel> participants) {
        this.registrationId = registrationId;
        this.status = status;
        this.createdAt = createdAt;
        this.timeCategory = timeCategory;
        this.event = event;
        this.participants = participants;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public RegistrationTimeCategory getTimeCategory() {
        return timeCategory;
    }

    public void setTimeCategory(RegistrationTimeCategory timeCategory) {
        this.timeCategory = timeCategory;
    }

    public RegistrationHistoryEventResponseModel getEvent() {
        return event;
    }

    public void setEvent(RegistrationHistoryEventResponseModel event) {
        this.event = event;
    }

    public java.util.List<ParticipantResponseModel> getParticipants() {
        return participants;
    }

    public void setParticipants(java.util.List<ParticipantResponseModel> participants) {
        this.participants = participants;
    }
}

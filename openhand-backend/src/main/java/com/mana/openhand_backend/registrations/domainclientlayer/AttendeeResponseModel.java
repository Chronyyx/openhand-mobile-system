package com.mana.openhand_backend.registrations.domainclientlayer;

public class AttendeeResponseModel {
    private Long userId;
    private String userName;
    private String userEmail;
    private String registrationStatus;
    private String memberStatus;
    private Integer waitlistedPosition;
    private String requestedAt;
    private String confirmedAt;
    private java.util.List<ParticipantResponseModel> participants;

    public AttendeeResponseModel() {
    }

    public AttendeeResponseModel(Long userId, String userName, String userEmail, 
                                String registrationStatus, String memberStatus,
                                Integer waitlistedPosition, String requestedAt, String confirmedAt,
                                java.util.List<ParticipantResponseModel> participants) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.registrationStatus = registrationStatus;
        this.memberStatus = memberStatus;
        this.waitlistedPosition = waitlistedPosition;
        this.requestedAt = requestedAt;
        this.confirmedAt = confirmedAt;
        this.participants = participants;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(String memberStatus) {
        this.memberStatus = memberStatus;
    }

    public Integer getWaitlistedPosition() {
        return waitlistedPosition;
    }

    public void setWaitlistedPosition(Integer waitlistedPosition) {
        this.waitlistedPosition = waitlistedPosition;
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

    public java.util.List<ParticipantResponseModel> getParticipants() {
        return participants;
    }

    public void setParticipants(java.util.List<ParticipantResponseModel> participants) {
        this.participants = participants;
    }
}

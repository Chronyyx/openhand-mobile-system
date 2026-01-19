package com.mana.openhand_backend.attendance.domainclientlayer;

public class AttendanceAttendeeResponseModel {
    private Long userId;
    private String fullName;
    private String email;
    private String registrationStatus;
    private boolean checkedIn;
    private String checkedInAt;

    public AttendanceAttendeeResponseModel() {
    }

    public AttendanceAttendeeResponseModel(
            Long userId,
            String fullName,
            String email,
            String registrationStatus,
            boolean checkedIn,
            String checkedInAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.registrationStatus = registrationStatus;
        this.checkedIn = checkedIn;
        this.checkedInAt = checkedInAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
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
}

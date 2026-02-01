package com.mana.openhand_backend.registrations.domainclientlayer;

public class ParticipantResponseModel {
    private Long registrationId;
    private String fullName;
    private Integer age;
    private String dateOfBirth;
    private String relation;
    private Boolean primaryRegistrant;
    private String status;
    private Integer waitlistedPosition;

    public ParticipantResponseModel() {
    }

    public ParticipantResponseModel(Long registrationId, String fullName, Integer age, String dateOfBirth,
                                    String relation, Boolean primaryRegistrant, String status,
                                    Integer waitlistedPosition) {
        this.registrationId = registrationId;
        this.fullName = fullName;
        this.age = age;
        this.dateOfBirth = dateOfBirth;
        this.relation = relation;
        this.primaryRegistrant = primaryRegistrant;
        this.status = status;
        this.waitlistedPosition = waitlistedPosition;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public Boolean getPrimaryRegistrant() {
        return primaryRegistrant;
    }

    public void setPrimaryRegistrant(Boolean primaryRegistrant) {
        this.primaryRegistrant = primaryRegistrant;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getWaitlistedPosition() {
        return waitlistedPosition;
    }

    public void setWaitlistedPosition(Integer waitlistedPosition) {
        this.waitlistedPosition = waitlistedPosition;
    }
}

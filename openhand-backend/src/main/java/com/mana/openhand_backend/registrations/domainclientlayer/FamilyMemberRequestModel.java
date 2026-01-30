package com.mana.openhand_backend.registrations.domainclientlayer;

public class FamilyMemberRequestModel {
    private String fullName;
    private Integer age;
    private String dateOfBirth;
    private String relation;

    public FamilyMemberRequestModel() {
    }

    public FamilyMemberRequestModel(String fullName, Integer age, String dateOfBirth, String relation) {
        this.fullName = fullName;
        this.age = age;
        this.dateOfBirth = dateOfBirth;
        this.relation = relation;
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
}

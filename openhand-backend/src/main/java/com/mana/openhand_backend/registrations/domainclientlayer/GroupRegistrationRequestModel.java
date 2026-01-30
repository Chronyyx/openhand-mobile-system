package com.mana.openhand_backend.registrations.domainclientlayer;

import java.util.ArrayList;
import java.util.List;

public class GroupRegistrationRequestModel {
    private Boolean primaryMember;
    private List<FamilyMemberRequestModel> familyMembers = new ArrayList<>();

    public GroupRegistrationRequestModel() {
    }

    public GroupRegistrationRequestModel(Boolean primaryMember, List<FamilyMemberRequestModel> familyMembers) {
        this.primaryMember = primaryMember;
        this.familyMembers = familyMembers != null ? familyMembers : new ArrayList<>();
    }

    public Boolean getPrimaryMember() {
        return primaryMember;
    }

    public void setPrimaryMember(Boolean primaryMember) {
        this.primaryMember = primaryMember;
    }

    public List<FamilyMemberRequestModel> getFamilyMembers() {
        return familyMembers;
    }

    public void setFamilyMembers(List<FamilyMemberRequestModel> familyMembers) {
        this.familyMembers = familyMembers != null ? familyMembers : new ArrayList<>();
    }
}

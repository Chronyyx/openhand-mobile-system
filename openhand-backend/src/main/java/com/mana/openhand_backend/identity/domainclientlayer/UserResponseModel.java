package com.mana.openhand_backend.identity.domainclientlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class UserResponseModel {
    private Long id;
    private String email;
    private Set<String> roles;

    private String name;
    private String phoneNumber;
    private String gender;
    private Integer age;
    private String memberStatus;
    private LocalDateTime statusChangedAt;

    public UserResponseModel(Long id, String email, Set<String> roles, String name, String phoneNumber, String gender,
            Integer age, String memberStatus, LocalDateTime statusChangedAt) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.age = age;
        this.memberStatus = memberStatus;
        this.statusChangedAt = statusChangedAt;
    }

    public static UserResponseModel fromEntity(User user) {
        return new UserResponseModel(
                user.getId(),
                user.getEmail(),
                new HashSet<>(user.getRoles()),
                user.getName(),
                user.getPhoneNumber(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getAge(),
                user.getMemberStatus() != null ? user.getMemberStatus().name() : "ACTIVE",
                user.getStatusChangedAt());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public String getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(String memberStatus) {
        this.memberStatus = memberStatus;
    }

    public LocalDateTime getStatusChangedAt() {
        return statusChangedAt;
    }

    public void setStatusChangedAt(LocalDateTime statusChangedAt) {
        this.statusChangedAt = statusChangedAt;
    }
}


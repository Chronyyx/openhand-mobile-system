package com.mana.openhand_backend.identity.domainclientlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;

import java.util.HashSet;
import java.util.Set;

public class UserResponseModel {
    private Long id;
    private String email;
    private Set<String> roles;

    public UserResponseModel(Long id, String email, Set<String> roles) {
        this.id = id;
        this.email = email;
        this.roles = roles;
    }

    public static UserResponseModel fromEntity(User user) {
        return new UserResponseModel(
                user.getId(),
                user.getEmail(),
                new HashSet<>(user.getRoles()));
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
}

package com.mana.openhand_backend.security.services;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;

    private String email;

    @JsonIgnore
    private String password;

    private boolean accountNonLocked;

    private Collection<? extends GrantedAuthority> authorities;
    private String name;
    private String phoneNumber;
    private String gender;
    private Integer age;
    private String profilePictureUrl;

    public UserDetailsImpl(Long id, String email, String password, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities, String name, String phoneNumber, String gender,
            Integer age, String profilePictureUrl) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.gender = gender;
        this.age = age;
        this.profilePictureUrl = profilePictureUrl;
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isAccountNonLocked(),
                authorities,
                user.getName(),
                user.getPhoneNumber(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getAge(),
                user.getProfilePictureUrl());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getGender() {
        return gender;
    }

    public Integer getAge() {
        return age;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(id, user.id);
    }
}

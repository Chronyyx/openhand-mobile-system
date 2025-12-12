package com.mana.openhand_backend.identity.presentationlayer.payload;

import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SignupRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Name is required")
    @jakarta.validation.constraints.Size(max = 50, message = "Name must be less than 50 characters")
    private String name;

    @jakarta.validation.constraints.Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be valid")
    private String phoneNumber;
    private com.mana.openhand_backend.identity.dataaccesslayer.Gender gender;

    @jakarta.validation.constraints.NotNull(message = "Age is required")
    @jakarta.validation.constraints.Min(value = 13, message = "Age must be at least 13")
    @jakarta.validation.constraints.Max(value = 120, message = "Age must be at most 120")
    private Integer age;

    private Set<String> roles;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
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

    public com.mana.openhand_backend.identity.dataaccesslayer.Gender getGender() {
        return gender;
    }

    public void setGender(com.mana.openhand_backend.identity.dataaccesslayer.Gender gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
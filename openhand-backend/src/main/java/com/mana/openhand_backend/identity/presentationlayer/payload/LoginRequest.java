package com.mana.openhand_backend.identity.presentationlayer.payload;

import com.mana.openhand_backend.identity.validation.EmailOrPhone;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    @EmailOrPhone
    private String email;

    @NotBlank
    private String password;

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
}
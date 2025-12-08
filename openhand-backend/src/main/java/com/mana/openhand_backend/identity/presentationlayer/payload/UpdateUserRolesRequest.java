package com.mana.openhand_backend.identity.presentationlayer.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public class UpdateUserRolesRequest {

    @NotNull(message = "Roles are required")
    @NotEmpty(message = "At least one role must be provided")
    private Set<@NotBlank(message = "Role value cannot be blank") String> roles;

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
}

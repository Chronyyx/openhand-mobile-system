package com.mana.openhand_backend.identity.presentationlayer.payload;

import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateUserStatusRequest {

    @NotNull(message = "Status is required")
    private MemberStatus status;

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }
}

package com.mana.openhand_backend.identity.presentationlayer.payload;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileRequest(
        @Size(max = 50) String name,
        @Pattern(regexp = "^$|\\+?[0-9]{10,15}$") String phoneNumber,
        String preferredLanguage,
        String gender,
        @Min(1) @Max(150) Integer age) {
}

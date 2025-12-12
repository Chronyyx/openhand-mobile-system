package com.mana.openhand_backend.identity.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class EmailOrPhoneValidator implements ConstraintValidator<EmailOrPhone, String> {

    // Regex for basic email validation
    private static final String EMAIL_PATTERN = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,}$";

    // Regex for digits-only phone number (allowing 10-15 digits)
    // We assume input might be raw digits or basic formatting,
    // but for "is valid phone" check we'll strip non-digits first.
    private static final String PHONE_PATTERN = "^\\+?[0-9]{10,15}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotBlank handle empty values
        }

        // Check if it matches email pattern
        if (Pattern.matches(EMAIL_PATTERN, value)) {
            return true;
        }

        // Check if it matches phone pattern (after stripping non-digits, preserving +)
        String digitsOnly = value.replaceAll("[^0-9+]", "");
        return Pattern.matches(PHONE_PATTERN, digitsOnly);
    }
}

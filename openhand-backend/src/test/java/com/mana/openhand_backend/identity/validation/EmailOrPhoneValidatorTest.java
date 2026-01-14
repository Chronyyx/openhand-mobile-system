package com.mana.openhand_backend.identity.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EmailOrPhoneValidatorTest {

    private final EmailOrPhoneValidator validator = new EmailOrPhoneValidator();
    private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

    @Test
    void isValid_allowsNullOrBlank() {
        assertTrue(validator.isValid(null, context));
        assertTrue(validator.isValid("   ", context));
    }

    @Test
    void isValid_acceptsValidEmail() {
        assertTrue(validator.isValid("user@example.com", context));
    }

    @Test
    void isValid_rejectsInvalidEmail() {
        assertFalse(validator.isValid("not-an-email", context));
    }

    @Test
    void isValid_acceptsPhoneNumbersWithFormatting() {
        assertTrue(validator.isValid("(438) 123-4567", context));
        assertTrue(validator.isValid("+1 514 555 1234", context));
    }

    @Test
    void isValid_rejectsTooShortPhoneNumbers() {
        assertFalse(validator.isValid("12345", context));
    }
}

package com.mana.openhand_backend.identity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailOrPhoneValidator.class)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailOrPhone {
    String message() default "Invalid email or phone number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

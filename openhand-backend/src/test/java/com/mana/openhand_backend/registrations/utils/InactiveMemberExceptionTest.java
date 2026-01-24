package com.mana.openhand_backend.registrations.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InactiveMemberExceptionTest {

    @Test
    void constructor_withUserId_buildsMessage() {
        InactiveMemberException ex = new InactiveMemberException(7L);

        assertTrue(ex.getMessage().contains("7"));
    }

    @Test
    void constructor_withMessage_usesCustomMessage() {
        InactiveMemberException ex = new InactiveMemberException("custom");

        assertEquals("custom", ex.getMessage());
    }
}

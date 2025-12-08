package com.mana.openhand_backend.events.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventNotFoundExceptionTest {

    @Test
    void constructor_setsCorrectMessage() {
        Long id = 42L;

        EventNotFoundException ex = new EventNotFoundException(id);

        assertEquals("Event with id 42 not found", ex.getMessage());
    }
}

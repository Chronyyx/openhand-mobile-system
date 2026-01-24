package com.mana.openhand_backend.events.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventAttendeeResponseModelTest {

    @Test
    void constructor_setsFields() {
        EventAttendeeResponseModel model = new EventAttendeeResponseModel(5L, "Jane Doe", 30);

        assertEquals(5L, model.getAttendeeId());
        assertEquals("Jane Doe", model.getFullName());
        assertEquals(30, model.getAge());
    }

    @Test
    void setters_updateFields() {
        EventAttendeeResponseModel model = new EventAttendeeResponseModel();

        model.setAttendeeId(2L);
        model.setFullName("Updated");
        model.setAge(null);

        assertEquals(2L, model.getAttendeeId());
        assertEquals("Updated", model.getFullName());
        assertNull(model.getAge());
    }
}

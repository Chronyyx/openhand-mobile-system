package com.mana.openhand_backend.attendance.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceAttendeeResponseModelTest {

    @Test
    void constructor_setsFields() {
        AttendanceAttendeeResponseModel model = new AttendanceAttendeeResponseModel(
                11L,
                "Jane Doe",
                "jane@example.com",
                "CONFIRMED",
                true,
                "2025-01-01T10:00"
        );

        assertEquals(11L, model.getUserId());
        assertEquals("Jane Doe", model.getFullName());
        assertEquals("jane@example.com", model.getEmail());
        assertEquals("CONFIRMED", model.getRegistrationStatus());
        assertTrue(model.isCheckedIn());
        assertEquals("2025-01-01T10:00", model.getCheckedInAt());
    }

    @Test
    void setters_updateFields() {
        AttendanceAttendeeResponseModel model = new AttendanceAttendeeResponseModel();

        model.setUserId(2L);
        model.setFullName("Updated");
        model.setEmail("updated@example.com");
        model.setRegistrationStatus("WAITLISTED");
        model.setCheckedIn(false);
        model.setCheckedInAt(null);

        assertEquals(2L, model.getUserId());
        assertEquals("Updated", model.getFullName());
        assertEquals("updated@example.com", model.getEmail());
        assertEquals("WAITLISTED", model.getRegistrationStatus());
        assertFalse(model.isCheckedIn());
        assertNull(model.getCheckedInAt());
    }
}

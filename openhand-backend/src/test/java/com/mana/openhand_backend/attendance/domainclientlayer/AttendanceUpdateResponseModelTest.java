package com.mana.openhand_backend.attendance.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceUpdateResponseModelTest {

    @Test
    void constructor_setsFields() {
        AttendanceUpdateResponseModel model = new AttendanceUpdateResponseModel(
                12L,
                99L,
                true,
                "2025-01-01T09:00",
                20,
                10,
                50.0
        );

        assertEquals(12L, model.getEventId());
        assertEquals(99L, model.getUserId());
        assertTrue(model.isCheckedIn());
        assertEquals("2025-01-01T09:00", model.getCheckedInAt());
        assertEquals(20, model.getRegisteredCount());
        assertEquals(10, model.getCheckedInCount());
        assertEquals(50.0, model.getOccupancyPercent());
    }

    @Test
    void setters_updateFields() {
        AttendanceUpdateResponseModel model = new AttendanceUpdateResponseModel();

        model.setEventId(1L);
        model.setUserId(2L);
        model.setCheckedIn(false);
        model.setCheckedInAt(null);
        model.setRegisteredCount(1);
        model.setCheckedInCount(0);
        model.setOccupancyPercent(null);

        assertEquals(1L, model.getEventId());
        assertEquals(2L, model.getUserId());
        assertFalse(model.isCheckedIn());
        assertNull(model.getCheckedInAt());
        assertEquals(1, model.getRegisteredCount());
        assertEquals(0, model.getCheckedInCount());
        assertNull(model.getOccupancyPercent());
    }
}

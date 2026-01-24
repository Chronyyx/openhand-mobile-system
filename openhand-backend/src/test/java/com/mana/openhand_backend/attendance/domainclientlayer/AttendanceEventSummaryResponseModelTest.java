package com.mana.openhand_backend.attendance.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceEventSummaryResponseModelTest {

    @Test
    void constructor_setsFields() {
        AttendanceEventSummaryResponseModel model = new AttendanceEventSummaryResponseModel(
                5L,
                "Event Title",
                "2025-02-02T09:00",
                "2025-02-02T10:00",
                "Main Hall",
                "123 Street",
                "OPEN",
                100,
                12,
                4,
                4.0
        );

        assertEquals(5L, model.getEventId());
        assertEquals("Event Title", model.getTitle());
        assertEquals("2025-02-02T09:00", model.getStartDateTime());
        assertEquals("2025-02-02T10:00", model.getEndDateTime());
        assertEquals("Main Hall", model.getLocationName());
        assertEquals("123 Street", model.getAddress());
        assertEquals("OPEN", model.getStatus());
        assertEquals(100, model.getMaxCapacity());
        assertEquals(12, model.getRegisteredCount());
        assertEquals(4, model.getCheckedInCount());
        assertEquals(4.0, model.getOccupancyPercent());
    }

    @Test
    void setters_updateFields() {
        AttendanceEventSummaryResponseModel model = new AttendanceEventSummaryResponseModel();

        model.setEventId(2L);
        model.setTitle("Updated");
        model.setStartDateTime("2025-03-01T11:00");
        model.setEndDateTime(null);
        model.setLocationName("Room B");
        model.setAddress("456 Road");
        model.setStatus("COMPLETED");
        model.setMaxCapacity(null);
        model.setRegisteredCount(0);
        model.setCheckedInCount(0);
        model.setOccupancyPercent(null);

        assertEquals(2L, model.getEventId());
        assertEquals("Updated", model.getTitle());
        assertEquals("2025-03-01T11:00", model.getStartDateTime());
        assertNull(model.getEndDateTime());
        assertEquals("Room B", model.getLocationName());
        assertEquals("456 Road", model.getAddress());
        assertEquals("COMPLETED", model.getStatus());
        assertNull(model.getMaxCapacity());
        assertEquals(0, model.getRegisteredCount());
        assertEquals(0, model.getCheckedInCount());
        assertNull(model.getOccupancyPercent());
    }
}

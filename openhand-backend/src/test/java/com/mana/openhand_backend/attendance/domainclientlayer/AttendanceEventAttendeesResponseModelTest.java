package com.mana.openhand_backend.attendance.domainclientlayer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceEventAttendeesResponseModelTest {

    @Test
    void constructor_setsFields() {
        List<AttendanceAttendeeResponseModel> attendees = List.of(
                new AttendanceAttendeeResponseModel(1L, "A", "a@test.com", "CONFIRMED", true, "2025-01-01T10:00")
        );

        AttendanceEventAttendeesResponseModel model = new AttendanceEventAttendeesResponseModel(
                9L,
                3,
                1,
                attendees
        );

        assertEquals(9L, model.getEventId());
        assertEquals(3, model.getRegisteredCount());
        assertEquals(1, model.getCheckedInCount());
        assertEquals(attendees, model.getAttendees());
    }

    @Test
    void setters_updateFields() {
        AttendanceEventAttendeesResponseModel model = new AttendanceEventAttendeesResponseModel();

        model.setEventId(2L);
        model.setRegisteredCount(5);
        model.setCheckedInCount(2);
        model.setAttendees(List.of());

        assertEquals(2L, model.getEventId());
        assertEquals(5, model.getRegisteredCount());
        assertEquals(2, model.getCheckedInCount());
        assertEquals(List.of(), model.getAttendees());
    }
}

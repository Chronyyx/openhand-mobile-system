package com.mana.openhand_backend.events.domainclientlayer;

import com.mana.openhand_backend.registrations.domainclientlayer.AttendeeResponseModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationSummaryResponseModelTest {

    @Test
    void constructor_setsFieldsWithoutAttendees() {
        RegistrationSummaryResponseModel model = new RegistrationSummaryResponseModel(
                4L,
                10,
                2,
                20,
                8,
                50.0
        );

        assertEquals(4L, model.getEventId());
        assertEquals(10, model.getTotalRegistrations());
        assertEquals(2, model.getWaitlistedCount());
        assertEquals(20, model.getMaxCapacity());
        assertEquals(8, model.getRemainingSpots());
        assertEquals(50.0, model.getPercentageFull());
        assertNull(model.getAttendees());
    }

    @Test
    void constructor_setsFieldsWithAttendees() {
        List<AttendeeResponseModel> attendees = List.of(
                new AttendeeResponseModel(1L, "A", "a@test.com", "CONFIRMED", "ACTIVE", null, null, null, null)
        );

        RegistrationSummaryResponseModel model = new RegistrationSummaryResponseModel(
                5L,
                1,
                0,
                null,
                null,
                null,
                attendees
        );

        assertEquals(5L, model.getEventId());
        assertEquals(1, model.getTotalRegistrations());
        assertEquals(attendees, model.getAttendees());
    }

    @Test
    void setters_updateFields() {
        RegistrationSummaryResponseModel model = new RegistrationSummaryResponseModel();

        model.setEventId(2L);
        model.setTotalRegistrations(3);
        model.setWaitlistedCount(1);
        model.setMaxCapacity(null);
        model.setRemainingSpots(null);
        model.setPercentageFull(null);
        model.setAttendees(List.of());

        assertEquals(2L, model.getEventId());
        assertEquals(3, model.getTotalRegistrations());
        assertEquals(1, model.getWaitlistedCount());
        assertNull(model.getMaxCapacity());
        assertNull(model.getRemainingSpots());
        assertNull(model.getPercentageFull());
        assertEquals(List.of(), model.getAttendees());
    }
}

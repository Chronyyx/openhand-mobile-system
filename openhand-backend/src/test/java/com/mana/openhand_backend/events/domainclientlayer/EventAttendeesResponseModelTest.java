package com.mana.openhand_backend.events.domainclientlayer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventAttendeesResponseModelTest {

    @Test
    void constructor_setsFields() {
        List<EventAttendeeResponseModel> attendees = List.of(
                new EventAttendeeResponseModel(1L, "A", 20)
        );

        EventAttendeesResponseModel model = new EventAttendeesResponseModel(9L, 1, attendees);

        assertEquals(9L, model.getEventId());
        assertEquals(1, model.getTotalAttendees());
        assertEquals(attendees, model.getAttendees());
    }

    @Test
    void setters_updateFields() {
        EventAttendeesResponseModel model = new EventAttendeesResponseModel();

        model.setEventId(2L);
        model.setTotalAttendees(0);
        model.setAttendees(List.of());

        assertEquals(2L, model.getEventId());
        assertEquals(0, model.getTotalAttendees());
        assertEquals(List.of(), model.getAttendees());
    }
}

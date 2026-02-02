package com.mana.openhand_backend.registrations.domainclientlayer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationResponseModelsTest {

    @Test
    void participantResponseModel_settersAndConstructor() {
        ParticipantResponseModel model = new ParticipantResponseModel();
        model.setRegistrationId(1L);
        model.setFullName("Test User");
        model.setAge(20);
        model.setDateOfBirth("2000-01-01");
        model.setRelation("Friend");
        model.setPrimaryRegistrant(true);
        model.setStatus("CONFIRMED");
        model.setWaitlistedPosition(2);

        assertEquals(1L, model.getRegistrationId());
        assertEquals("Test User", model.getFullName());
        assertEquals(20, model.getAge());
        assertEquals("2000-01-01", model.getDateOfBirth());
        assertEquals("Friend", model.getRelation());
        assertTrue(model.getPrimaryRegistrant());
        assertEquals("CONFIRMED", model.getStatus());
        assertEquals(2, model.getWaitlistedPosition());

        ParticipantResponseModel constructed = new ParticipantResponseModel(2L, "Other", 30,
                "1994-01-01", "Sibling", false, "WAITLISTED", 1);
        assertEquals("Other", constructed.getFullName());
    }

    @Test
    void groupRegistrationResponseModel_handlesNullParticipants() {
        GroupRegistrationResponseModel model = new GroupRegistrationResponseModel();
        model.setParticipants(null);
        assertNotNull(model.getParticipants());
        assertTrue(model.getParticipants().isEmpty());

        GroupRegistrationResponseModel constructed = new GroupRegistrationResponseModel(10L, null, null, 5);
        assertNotNull(constructed.getParticipants());
        assertTrue(constructed.getParticipants().isEmpty());
    }

    @Test
    void groupRegistrationResponseModel_setters() {
        ParticipantResponseModel participant = new ParticipantResponseModel();
        GroupRegistrationResponseModel model = new GroupRegistrationResponseModel();
        model.setEventId(12L);
        model.setPrimaryRegistrant(participant);
        model.setParticipants(List.of(participant));
        model.setRemainingCapacity(3);

        assertEquals(12L, model.getEventId());
        assertEquals(participant, model.getPrimaryRegistrant());
        assertEquals(1, model.getParticipants().size());
        assertEquals(3, model.getRemainingCapacity());
    }

    @Test
    void attendeeResponseModel_setters() {
        AttendeeResponseModel model = new AttendeeResponseModel();
        model.setUserId(1L);
        model.setRequestedAt("2025-01-01T10:00");
        model.setConfirmedAt("2025-01-01T10:01");
        model.setParticipants(List.of(new ParticipantResponseModel()));

        assertEquals(1L, model.getUserId());
        assertEquals("2025-01-01T10:00", model.getRequestedAt());
        assertEquals("2025-01-01T10:01", model.getConfirmedAt());
        assertEquals(1, model.getParticipants().size());
    }

    @Test
    void registrationHistoryResponseModel_setParticipants() {
        RegistrationHistoryResponseModel model = new RegistrationHistoryResponseModel();
        model.setParticipants(List.of(new ParticipantResponseModel()));
        assertEquals(1, model.getParticipants().size());
    }
}

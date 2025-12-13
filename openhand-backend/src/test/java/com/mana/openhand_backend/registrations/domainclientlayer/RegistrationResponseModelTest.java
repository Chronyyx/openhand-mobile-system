package com.mana.openhand_backend.registrations.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationResponseModelTest {

    @Test
    void constructor_withAllParameters_shouldCreateModel() {
        Long id = 1L;
        Long userId = 100L;
        Long eventId = 10L;
        String eventTitle = "Test Event";
        String status = "CONFIRMED";
        String requestedAt = "2025-01-15T10:00:00";
        String confirmedAt = "2025-01-15T11:00:00";
        String cancelledAt = null;
        Integer waitlistedPosition = null;
        String eventStartDateTime = "2025-01-20T09:00:00";
        String eventEndDateTime = "2025-01-20T11:00:00";
        RegistrationResponseModel model = new RegistrationResponseModel(
                id, userId, eventId, eventTitle, status, requestedAt, confirmedAt,
                cancelledAt, waitlistedPosition, eventStartDateTime, eventEndDateTime
        );
        assertNotNull(model);
        assertEquals(id, model.getId());
        assertEquals(userId, model.getUserId());
        assertEquals(eventId, model.getEventId());
        assertEquals(eventTitle, model.getEventTitle());
        assertEquals(status, model.getStatus());
        assertEquals(requestedAt, model.getRequestedAt());
        assertEquals(confirmedAt, model.getConfirmedAt());
        assertNull(model.getCancelledAt());
        assertNull(model.getWaitlistedPosition());
        assertEquals(eventStartDateTime, model.getEventStartDateTime());
        assertEquals(eventEndDateTime, model.getEventEndDateTime());
    }

    @Test
    void constructor_noArgs_shouldCreateEmptyModel() {
        RegistrationResponseModel model = new RegistrationResponseModel();
        assertNotNull(model);
        assertNull(model.getId());
        assertNull(model.getUserId());
        assertNull(model.getEventId());
    }

    @Test
    void setters_shouldUpdateValues() {
        RegistrationResponseModel model = new RegistrationResponseModel();
        model.setId(1L);
        model.setUserId(100L);
        model.setEventId(10L);
        model.setEventTitle("Updated Event");
        model.setStatus("CANCELLED");
        model.setRequestedAt("2025-01-15T10:00:00");
        model.setConfirmedAt("2025-01-15T11:00:00");
        model.setCancelledAt("2025-01-16T12:00:00");
        model.setWaitlistedPosition(5);
        assertEquals(1L, model.getId());
        assertEquals(100L, model.getUserId());
        assertEquals(10L, model.getEventId());
        assertEquals("Updated Event", model.getEventTitle());
        assertEquals("CANCELLED", model.getStatus());
        assertEquals("2025-01-15T10:00:00", model.getRequestedAt());
        assertEquals("2025-01-15T11:00:00", model.getConfirmedAt());
        assertEquals("2025-01-16T12:00:00", model.getCancelledAt());
        assertEquals(5, model.getWaitlistedPosition());
    }

    @Test
    void getters_withNullValues_shouldReturnNull() {
        RegistrationResponseModel model = new RegistrationResponseModel(
                null, null, null, null, null, null, null, null, null, null, null
        );
        assertNull(model.getId());
        assertNull(model.getUserId());
        assertNull(model.getEventId());
        assertNull(model.getEventTitle());
        assertNull(model.getStatus());
        assertNull(model.getRequestedAt());
        assertNull(model.getConfirmedAt());
        assertNull(model.getCancelledAt());
        assertNull(model.getWaitlistedPosition());
        assertNull(model.getEventStartDateTime());
        assertNull(model.getEventEndDateTime());
    }

    @Test
    void status_CONFIRMED_shouldReturnConfirmed() {
        RegistrationResponseModel model = new RegistrationResponseModel(
                1L, 100L, 10L, "Event", "CONFIRMED", "2025-01-15T10:00:00",
                "2025-01-15T11:00:00", null, null, "2025-02-01T09:00:00", "2025-02-01T10:00:00"
        );
        assertEquals("CONFIRMED", model.getStatus());
    }

    @Test
    void status_CANCELLED_shouldReturnCancelled() {
        RegistrationResponseModel model = new RegistrationResponseModel(
                1L, 100L, 10L, "Event", "CANCELLED", "2025-01-15T10:00:00",
                null, "2025-01-16T12:00:00", null, "2025-02-01T09:00:00", "2025-02-01T10:00:00"
        );
        assertEquals("CANCELLED", model.getStatus());
    }

    @Test
    void multipleInstances_shouldBeIndependent() {
        RegistrationResponseModel model1 = new RegistrationResponseModel(
                1L, 100L, 10L, "Event 1", "CONFIRMED", "2025-01-15T10:00:00",
                "2025-01-15T11:00:00", null, null, "2025-02-01T09:00:00", "2025-02-01T10:00:00"
        );
        RegistrationResponseModel model2 = new RegistrationResponseModel(
                2L, 200L, 20L, "Event 2", "CANCELLED", "2025-02-15T10:00:00",
                null, "2025-02-16T12:00:00", null, "2025-03-01T09:00:00", "2025-03-01T10:00:00"
        );
        assertNotEquals(model1.getId(), model2.getId());
        assertNotEquals(model1.getUserId(), model2.getUserId());
        assertNotEquals(model1.getStatus(), model2.getStatus());
    }

    @Test
    void setStatus_withDifferentValues_shouldUpdateCorrectly() {
        RegistrationResponseModel model = new RegistrationResponseModel();
        model.setStatus("CONFIRMED");
        assertEquals("CONFIRMED", model.getStatus());
        model.setStatus("CANCELLED");
        assertEquals("CANCELLED", model.getStatus());
        model.setStatus("CONFIRMED");
        assertEquals("CONFIRMED", model.getStatus());
    }

    @Test
    void setEventTitle_withEmptyString_shouldAccept() {
        RegistrationResponseModel model = new RegistrationResponseModel();
        model.setEventTitle("");
        assertEquals("", model.getEventTitle());
    }

    @Test
    void setEventTitle_withLongString_shouldAccept() {
        RegistrationResponseModel model = new RegistrationResponseModel();
        String longTitle = "A".repeat(500);
        model.setEventTitle(longTitle);
        assertEquals(longTitle, model.getEventTitle());
    }

    @Test
    void requestedAt_withVariousFormats_shouldAccept() {
        String[] dateTimes = {
                "2025-01-01T00:00:00",
                "2025-12-31T23:59:59",
                "2024-06-15T12:30:00"
        };
        for (String dateTime : dateTimes) {
            RegistrationResponseModel model = new RegistrationResponseModel();
            model.setRequestedAt(dateTime);
            assertEquals(dateTime, model.getRequestedAt());
        }
    }

    @Test
    void waitlistedPosition_withVariousValues_shouldAccept() {
        Integer[] positions = {0, 1, 5, 10, 100, 999};
        for (Integer position : positions) {
            RegistrationResponseModel model = new RegistrationResponseModel();
            model.setWaitlistedPosition(position);
            assertEquals(position, model.getWaitlistedPosition());
        }
    }

    @Test
    void model_withAllFieldsSet_shouldPreserveAllData() {
        RegistrationResponseModel model = new RegistrationResponseModel(
                123L, 456L, 789L, "Gala Event 2025", "CONFIRMED",
                "2025-01-15T18:00:00", "2025-01-15T18:05:00", null, null,
                "2025-01-20T09:00:00", "2025-01-20T11:00:00"
        );
        assertEquals(123L, model.getId());
        assertEquals(456L, model.getUserId());
        assertEquals(789L, model.getEventId());
        assertEquals("Gala Event 2025", model.getEventTitle());
        assertEquals("CONFIRMED", model.getStatus());
        assertEquals("2025-01-15T18:00:00", model.getRequestedAt());
        assertEquals("2025-01-15T18:05:00", model.getConfirmedAt());
        assertNull(model.getCancelledAt());
        assertNull(model.getWaitlistedPosition());
        assertEquals("2025-01-20T09:00:00", model.getEventStartDateTime());
        assertEquals("2025-01-20T11:00:00", model.getEventEndDateTime());
    }

    @Test
    void model_withWaitlistedData_shouldPreservePosition() {
        RegistrationResponseModel model = new RegistrationResponseModel(
                1L, 100L, 10L, "Event", "WAITLISTED", "2025-01-15T10:00:00",
                null, null, 3, "2025-02-01T09:00:00", "2025-02-01T10:00:00"
        );
        assertEquals("WAITLISTED", model.getStatus());
        assertEquals(3, model.getWaitlistedPosition());
    }

    @Test
    void model_afterCancellation_shouldHaveCancelledAt() {
        RegistrationResponseModel model = new RegistrationResponseModel();
        model.setId(1L);
        model.setStatus("CANCELLED");
        model.setCancelledAt("2025-01-16T12:00:00");
        assertEquals("CANCELLED", model.getStatus());
        assertEquals("2025-01-16T12:00:00", model.getCancelledAt());
    }

    @Test
    void eventDateTimes_shouldBeStoredAndReturned() {
        RegistrationResponseModel model = new RegistrationResponseModel();
        model.setEventStartDateTime("2025-04-01T09:00:00");
        model.setEventEndDateTime("2025-04-01T11:00:00");

        assertEquals("2025-04-01T09:00:00", model.getEventStartDateTime());
        assertEquals("2025-04-01T11:00:00", model.getEventEndDateTime());
    }
}

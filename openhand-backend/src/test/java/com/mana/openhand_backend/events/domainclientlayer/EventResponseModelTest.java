package com.mana.openhand_backend.events.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventResponseModelTest {

    @Test
    void noArgsConstructor_allowsSettingAndGettingAllFields() {
        EventResponseModel model = new EventResponseModel();

        model.setId(1L);
        model.setTitle("Title");
        model.setDescription("Description");
        model.setStartDateTime("2025-01-01T10:00");
        model.setEndDateTime("2025-01-01T12:00");
        model.setLocationName("Location");
        model.setAddress("123 Street");
        model.setStatus("OPEN");
        model.setMaxCapacity(50);
        model.setCurrentRegistrations(10);

        assertEquals(1L, model.getId());
        assertEquals("Title", model.getTitle());
        assertEquals("Description", model.getDescription());
        assertEquals("2025-01-01T10:00", model.getStartDateTime());
        assertEquals("2025-01-01T12:00", model.getEndDateTime());
        assertEquals("Location", model.getLocationName());
        assertEquals("123 Street", model.getAddress());
        assertEquals("OPEN", model.getStatus());
        assertEquals(50, model.getMaxCapacity());
        assertEquals(10, model.getCurrentRegistrations());
    }

    @Test
    void allArgsConstructor_setsAllFieldsCorrectly() {
        Long id = 2L;
        String title = "Event 2";
        String description = "Another description";
        String start = "2025-02-02T09:00";
        String end = "2025-02-02T11:00";
        String location = "Community Center";
        String address = "456 Avenue";
        String status = "CLOSED";
        Integer maxCapacity = 100;
        Integer currentRegistrations = 75;

        EventResponseModel model = new EventResponseModel(
                id,
                title,
                description,
                start,
                end,
                location,
                address,
                status,
                maxCapacity,
                currentRegistrations,
                "General");

        assertEquals(id, model.getId());
        assertEquals(title, model.getTitle());
        assertEquals(description, model.getDescription());
        assertEquals(start, model.getStartDateTime());
        assertEquals(end, model.getEndDateTime());
        assertEquals(location, model.getLocationName());
        assertEquals(address, model.getAddress());
        assertEquals(status, model.getStatus());
        assertEquals(maxCapacity, model.getMaxCapacity());
        assertEquals(currentRegistrations, model.getCurrentRegistrations());
    }
}

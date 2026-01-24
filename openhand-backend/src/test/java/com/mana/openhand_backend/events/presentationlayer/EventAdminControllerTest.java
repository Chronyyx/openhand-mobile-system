package com.mana.openhand_backend.events.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.businesslayer.EventAdminService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventAdminService eventAdminService;

    @Test
    void createEvent_returnsCreated() throws Exception {
        Event event = buildEvent(1L);
        when(eventAdminService.createEvent(any(CreateEventRequest.class))).thenReturn(event);

        mockMvc.perform(post("/api/admin/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    void createEvent_whenInvalidBody_returnsBadRequest() throws Exception {
        CreateEventRequest request = validRequest();
        request.setTitle(" ");

        mockMvc.perform(post("/api/admin/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_whenMissing_returnsNotFound() throws Exception {
        when(eventAdminService.updateEvent(any(Long.class), any(CreateEventRequest.class)))
                .thenThrow(new NoSuchElementException("missing"));

        mockMvc.perform(put("/api/admin/events/99")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEvent_whenInvalid_returnsBadRequest() throws Exception {
        when(eventAdminService.updateEvent(any(Long.class), any(CreateEventRequest.class)))
                .thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(put("/api/admin/events/2")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelEvent_whenMissing_returnsNotFound() throws Exception {
        when(eventAdminService.cancelEvent(7L)).thenThrow(new NoSuchElementException("missing"));

        mockMvc.perform(post("/api/admin/events/7/cancel"))
                .andExpect(status().isNotFound());
    }

    private CreateEventRequest validRequest() {
        CreateEventRequest request = new CreateEventRequest();
        request.setTitle("Test Event");
        request.setDescription("Desc");
        request.setStartDateTime("2025-01-01T09:00");
        request.setEndDateTime("2025-01-01T10:00");
        request.setLocationName("Hall");
        request.setAddress("123 Street");
        request.setMaxCapacity(10);
        request.setCategory("CATEGORY");
        return request;
    }

    private Event buildEvent(Long id) {
        Event event = new Event(
                "Test Event",
                "Desc",
                LocalDateTime.of(2025, 1, 1, 9, 0),
                LocalDateTime.of(2025, 1, 1, 10, 0),
                "Hall",
                "123 Street",
                EventStatus.OPEN,
                10,
                0,
                "CATEGORY"
        );
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}

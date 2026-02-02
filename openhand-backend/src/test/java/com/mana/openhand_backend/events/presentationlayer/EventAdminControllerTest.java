package com.mana.openhand_backend.events.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.businesslayer.EventAdminService;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.events.presentationlayer.payload.CreateEventRequest;
import com.mana.openhand_backend.common.presentationlayer.payload.ImageUrlResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @Test
    void cancelEvent_returnsEvent() throws Exception {
        Event event = buildEvent(3L);
        when(eventAdminService.cancelEvent(3L)).thenReturn(event);

        mockMvc.perform(post("/api/admin/events/3/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    void uploadEventImage_returnsUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        when(eventAdminService.uploadEventImage(eq(5L), any(MockMultipartFile.class), any(String.class)))
                .thenReturn(new ImageUrlResponse("http://localhost/uploads/test.jpg"));

        mockMvc.perform(multipart("/api/admin/events/5/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost/uploads/test.jpg"));
    }

    @Test
    void uploadEventImage_whenMissing_returnsNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        when(eventAdminService.uploadEventImage(eq(9L), any(MockMultipartFile.class), any(String.class)))
                .thenThrow(new NoSuchElementException("missing"));

        mockMvc.perform(multipart("/api/admin/events/9/image").file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadEventImage_whenInvalid_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "data".getBytes());
        when(eventAdminService.uploadEventImage(eq(10L), any(MockMultipartFile.class), any(String.class)))
                .thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(multipart("/api/admin/events/10/image").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEventImage_returnsUrl() throws Exception {
        when(eventAdminService.getEventImage(eq(11L), any(String.class)))
                .thenReturn(new ImageUrlResponse("http://localhost/uploads/11.jpg"));

        mockMvc.perform(get("/api/admin/events/11/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost/uploads/11.jpg"));
    }

    @Test
    void getEventImage_whenMissing_returnsNotFound() throws Exception {
        when(eventAdminService.getEventImage(eq(12L), any(String.class)))
                .thenThrow(new NoSuchElementException("missing"));

        mockMvc.perform(get("/api/admin/events/12/image"))
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

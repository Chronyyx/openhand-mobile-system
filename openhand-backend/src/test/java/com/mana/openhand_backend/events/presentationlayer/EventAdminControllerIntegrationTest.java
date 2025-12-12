package com.mana.openhand_backend.events.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class EventAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    private String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    @Test
    void createEvent_unauthenticated_returns401() throws Exception {
        String payload = """
                {
                  "title": "Admin Created Event",
                  "description": "Created from admin UI",
                  "startDateTime": "2026-01-01T18:00",
                  "endDateTime": "2026-01-01T20:00",
                  "locationName": "MANA",
                  "address": "Montréal, QC",
                  "maxCapacity": 50,
                  "category": "GENERAL"
                }
                """;

        mockMvc.perform(post("/api/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void createEvent_asNonAdmin_returns403() throws Exception {
        String payload = """
                {
                  "title": "Admin Created Event",
                  "description": "Created from admin UI",
                  "startDateTime": "2026-01-01T18:00",
                  "endDateTime": "2026-01-01T20:00",
                  "locationName": "MANA",
                  "address": "Montréal, QC",
                  "maxCapacity": 50,
                  "category": "GENERAL"
                }
                """;

        mockMvc.perform(post("/api/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void createEvent_withValidPayload_returns201AndPersists() throws Exception {
        String payload = """
                {
                  "title": "Admin Created Event",
                  "description": "Created from admin UI",
                  "startDateTime": "2026-01-01T18:00",
                  "endDateTime": "2026-01-01T20:00",
                  "locationName": "MANA",
                  "address": "Montréal, QC",
                  "maxCapacity": 50,
                  "category": "GENERAL"
                }
                """;

        mockMvc.perform(post("/api/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", equalTo("Admin Created Event")))
                .andExpect(jsonPath("$.description", equalTo("Created from admin UI")))
                .andExpect(jsonPath("$.locationName", equalTo("MANA")))
                .andExpect(jsonPath("$.address", equalTo("Montréal, QC")))
                .andExpect(jsonPath("$.status", equalTo("OPEN")))
                .andExpect(jsonPath("$.currentRegistrations", equalTo(0)))
                .andExpect(jsonPath("$.maxCapacity", equalTo(50)))
                .andExpect(jsonPath("$.category", equalTo("GENERAL")));

        assertEquals(1, eventRepository.count());
        Event stored = eventRepository.findAll().get(0);
        assertEquals("Admin Created Event", stored.getTitle());
        assertEquals(EventStatus.OPEN, stored.getStatus());
        assertEquals(Integer.valueOf(0), stored.getCurrentRegistrations());
        assertEquals(Integer.valueOf(50), stored.getMaxCapacity());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void createEvent_withMissingTitle_returns400() throws Exception {
        String payload = """
                {
                  "description": "Missing title",
                  "startDateTime": "2026-01-01T18:00",
                  "locationName": "MANA",
                  "address": "Montréal, QC"
                }
                """;

        mockMvc.perform(post("/api/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertEquals(0, eventRepository.count());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void createEvent_withEndBeforeStart_returns400() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 18, 0);
        LocalDateTime end = start.minusHours(1);

        String payload = asJsonString(new Object() {
            public final String title = "Bad event";
            public final String description = "Bad";
            public final String startDateTime = start.toString();
            public final String endDateTime = end.toString();
            public final String locationName = "MANA";
            public final String address = "Montréal, QC";
        });

        mockMvc.perform(post("/api/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertEquals(0, eventRepository.count());
    }
}

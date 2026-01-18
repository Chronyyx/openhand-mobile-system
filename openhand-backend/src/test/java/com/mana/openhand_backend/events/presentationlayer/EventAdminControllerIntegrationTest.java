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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateEvent_withValidPayload_updatesAndReturns200() throws Exception {
        LocalDateTime start = LocalDateTime.now()
                .plusDays(1)
                .withHour(18)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime end = start.plusHours(2);
        LocalDateTime updatedStart = start.plusDays(30);
        LocalDateTime updatedEnd = updatedStart.plusHours(3);

        Event existing = new Event(
                "Existing",
                "Desc",
                start,
                end,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                4,
                "GENERAL");
        Event saved = eventRepository.save(existing);

        String payload = asJsonString(new Object() {
            public final String title = "Updated Title";
            public final String description = "Updated Desc";
            public final String startDateTime = updatedStart.toString();
            public final String endDateTime = updatedEnd.toString();
            public final String locationName = "New Loc";
            public final String address = "New Addr";
            public final Integer maxCapacity = 5;
            public final String category = "NEW";
        });

        mockMvc.perform(put("/api/admin/events/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Updated Title")))
                .andExpect(jsonPath("$.status", equalTo("NEARLY_FULL")))
                .andExpect(jsonPath("$.maxCapacity", equalTo(5)))
                .andExpect(jsonPath("$.category", equalTo("NEW")));

        Event updated = eventRepository.findById(saved.getId()).orElseThrow();
        assertEquals("Updated Title", updated.getTitle());
        assertEquals(EventStatus.NEARLY_FULL, updated.getStatus());
        assertEquals(Integer.valueOf(5), updated.getMaxCapacity());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateEvent_whenNotFound_returns404() throws Exception {
        String payload = """
                {
                  "title": "Updated",
                  "description": "Updated Desc",
                  "startDateTime": "2026-02-01T18:00",
                  "endDateTime": "2026-02-01T21:00",
                  "locationName": "New Loc",
                  "address": "New Addr",
                  "maxCapacity": 5
                }
                """;

        mockMvc.perform(put("/api/admin/events/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateEvent_withMaxCapacityLessThanCurrent_returns400() throws Exception {
        LocalDateTime start = LocalDateTime.now()
                .plusDays(1)
                .withHour(18)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime end = start.plusHours(2);
        LocalDateTime updatedStart = start.plusDays(30);
        LocalDateTime updatedEnd = updatedStart.plusHours(3);

        Event existing = new Event(
                "Existing",
                "Desc",
                start,
                end,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                3,
                "GENERAL");
        Event saved = eventRepository.save(existing);

        String payload = asJsonString(new Object() {
            public final String title = "Updated";
            public final String description = "Updated Desc";
            public final String startDateTime = updatedStart.toString();
            public final String endDateTime = updatedEnd.toString();
            public final String locationName = "New Loc";
            public final String address = "New Addr";
            public final Integer maxCapacity = 2;
        });

        mockMvc.perform(put("/api/admin/events/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        Event unchanged = eventRepository.findById(saved.getId()).orElseThrow();
        assertEquals(Integer.valueOf(10), unchanged.getMaxCapacity());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void updateEvent_withEndBeforeStart_returns400() throws Exception {
        LocalDateTime start = LocalDateTime.now()
                .plusDays(1)
                .withHour(18)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        LocalDateTime end = start.plusHours(2);
        LocalDateTime updatedStart = start.plusDays(30);
        LocalDateTime updatedEnd = updatedStart.minusHours(1);

        Event existing = new Event(
                "Existing",
                "Desc",
                start,
                end,
                "Loc",
                "Addr",
                EventStatus.OPEN,
                10,
                2,
                "GENERAL");
        Event saved = eventRepository.save(existing);

        String payload = asJsonString(new Object() {
            public final String title = "Updated";
            public final String description = "Updated Desc";
            public final String startDateTime = updatedStart.toString();
            public final String endDateTime = updatedEnd.toString();
            public final String locationName = "New Loc";
            public final String address = "New Addr";
            public final Integer maxCapacity = 10;
        });

        mockMvc.perform(put("/api/admin/events/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}

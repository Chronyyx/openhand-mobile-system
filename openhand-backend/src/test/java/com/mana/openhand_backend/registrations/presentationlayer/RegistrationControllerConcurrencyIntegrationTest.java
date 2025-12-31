package com.mana.openhand_backend.registrations.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationRequestModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class RegistrationControllerConcurrencyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private Event event;
    private User user;

    @BeforeEach
    @Transactional
    void setup() {
        userRepository.deleteAll();
        eventRepository.deleteAll();

        user = new User();
        user.setEmail("concurrency@example.com");
        user.setPasswordHash("pw");
        user = userRepository.save(user);

        event = new Event(
                "Concurrency Event",
                "",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                "Loc",
                "",
                EventStatus.OPEN,
                1,
                0,
                "General");
        event = eventRepository.save(event);
    }

    private String asJson(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    @Transactional
    void twoSequentialRegistrations_onSingleCapacity_eventConfirmThenWaitlist() throws Exception {
        // Arrange
        RegistrationRequestModel request = new RegistrationRequestModel(event.getId());
        User second = new User();
        second.setEmail("concurrency2@example.com");
        second.setPasswordHash("pw");
        userRepository.save(second);

        // Act & Assert - first registration confirmed
        mockMvc.perform(post("/api/registrations")
                .with(csrf())
                .with(user("concurrency@example.com").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.status", equalTo("CONFIRMED")));

        // Act & Assert - second registration waitlisted
        mockMvc.perform(post("/api/registrations")
                .with(csrf())
                .with(user("concurrency2@example.com").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("WAITLISTED")));
    }
}

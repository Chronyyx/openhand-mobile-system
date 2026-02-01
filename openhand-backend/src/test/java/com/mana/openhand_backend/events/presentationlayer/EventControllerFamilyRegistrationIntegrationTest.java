package com.mana.openhand_backend.events.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreferenceRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.domainclientlayer.FamilyMemberRequestModel;
import com.mana.openhand_backend.registrations.domainclientlayer.GroupRegistrationRequestModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class EventControllerFamilyRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    @Transactional
    void setUp() {
        eventRepository.deleteAll();
        registrationRepository.deleteAll();
        notificationPreferenceRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("familymember@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setName("Primary Member");
        testUser = userRepository.save(testUser);

        testEvent = new Event(
                "Family Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                5,
                0,
                "General");
        testEvent = eventRepository.save(testEvent);
    }

    private String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    @Test
    @WithMockUser(username = "familymember@example.com", roles = "MEMBER")
    @Transactional
    void registerWithFamily_shouldReturnParticipantsAndUpdateCapacity() throws Exception {
        GroupRegistrationRequestModel request = new GroupRegistrationRequestModel(true,
                List.of(
                        new FamilyMemberRequestModel("Jane Doe", 12, null, "Child"),
                        new FamilyMemberRequestModel("Mark Doe", 8, null, "Child")
                ));

        mockMvc.perform(post("/api/events/" + testEvent.getId() + "/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId", equalTo(testEvent.getId().intValue())))
                .andExpect(jsonPath("$.participants", hasSize(3)))
                .andExpect(jsonPath("$.primaryRegistrant.fullName", equalTo("Primary Member")));

        Event updated = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertEquals(3, updated.getCurrentRegistrations());

        List<Registration> registrations = registrationRepository.findByEventId(testEvent.getId());
        assertEquals(3, registrations.size());
        String groupId = registrations.get(0).getRegistrationGroupId();
        assertTrue(registrations.stream().allMatch(reg -> groupId != null && groupId.equals(reg.getRegistrationGroupId())));
        assertTrue(registrations.stream().allMatch(reg -> reg.getPrimaryUserId() != null));
    }

    @Test
    @WithMockUser(username = "familymember@example.com", roles = "MEMBER")
    @Transactional
    void registerWithFamily_whenCapacityInsufficient_shouldReturn400() throws Exception {
        Event limitedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        limitedEvent.setMaxCapacity(2);
        limitedEvent.setCurrentRegistrations(1);
        eventRepository.save(limitedEvent);

        GroupRegistrationRequestModel request = new GroupRegistrationRequestModel(true,
                List.of(
                        new FamilyMemberRequestModel("Jane Doe", 12, null, "Child"),
                        new FamilyMemberRequestModel("Mark Doe", 8, null, "Child")
                ));

        mockMvc.perform(post("/api/events/" + testEvent.getId() + "/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("capacity")));
    }

    @Test
    @WithMockUser(username = "familymember@example.com", roles = "MEMBER")
    @Transactional
    void cancelRegistration_shouldRestoreCapacityForFamilyGroup() throws Exception {
        GroupRegistrationRequestModel request = new GroupRegistrationRequestModel(true,
                List.of(
                        new FamilyMemberRequestModel("Jane Doe", 12, null, "Child"),
                        new FamilyMemberRequestModel("Mark Doe", 8, null, "Child")
                ));

        mockMvc.perform(post("/api/events/" + testEvent.getId() + "/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        Event updated = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertEquals(3, updated.getCurrentRegistrations());

        mockMvc.perform(delete("/api/registrations/event/" + testEvent.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("CANCELLED")));

        Event afterCancel = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertEquals(0, afterCancel.getCurrentRegistrations());
    }
}

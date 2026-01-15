package com.mana.openhand_backend.registrations.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreferenceRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.EmployeeRegistrationRequestModel;
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
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class EmployeeRegistrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    private User employeeUser;
    private User participantUser;
    private Event testEvent;
    private LocalDateTime eventStart;
    private LocalDateTime eventEnd;

    @BeforeEach
    @Transactional
    void setUp() {
        eventStart = LocalDateTime.now().plusDays(1);
        eventEnd = LocalDateTime.now().plusDays(2);

        registrationRepository.deleteAll();
        registrationRepository.flush();
        eventRepository.deleteAll();
        eventRepository.flush();
        notificationPreferenceRepository.deleteAll();
        notificationPreferenceRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();

        employeeUser = new User();
        employeeUser.setEmail("employee@example.com");
        employeeUser.setPasswordHash("hashedPassword");
        employeeUser.setRoles(new HashSet<>());
        employeeUser = userRepository.saveAndFlush(employeeUser);

        participantUser = new User();
        participantUser.setEmail("participant@example.com");
        participantUser.setPasswordHash("hashedPassword");
        participantUser.setRoles(new HashSet<>());
        participantUser = userRepository.saveAndFlush(participantUser);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                eventStart,
                eventEnd,
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                2,
                0,
                "General"
        );
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
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void registerParticipant_withValidRequest_shouldReturn201Created() throws Exception {
        // Arrange
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(participantUser.getId(), testEvent.getId());

        // Act & Assert
        mockMvc.perform(post("/api/employee/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.userId", equalTo(participantUser.getId().intValue())))
                .andExpect(jsonPath("$.eventId", equalTo(testEvent.getId().intValue())))
                .andExpect(jsonPath("$.status", equalTo("CONFIRMED")))
                .andExpect(jsonPath("$.eventTitle", equalTo("Test Event")));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void registerParticipant_whenEventAtCapacity_shouldReturn201WithWaitlistedStatus() throws Exception {
        // Arrange
        Event filledEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        filledEvent.setCurrentRegistrations(2);
        filledEvent.setStatus(EventStatus.FULL);
        eventRepository.save(filledEvent);

        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(participantUser.getId(), testEvent.getId());

        // Act & Assert
        mockMvc.perform(post("/api/employee/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("WAITLISTED")))
                .andExpect(jsonPath("$.waitlistedPosition", equalTo(1)));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void registerParticipant_whenDuplicate_shouldReturn409Conflict() throws Exception {
        // Arrange
        Registration existingReg = new Registration(participantUser, testEvent);
        existingReg.setStatus(RegistrationStatus.CONFIRMED);
        existingReg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(existingReg);

        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(participantUser.getId(), testEvent.getId());

        // Act & Assert
        mockMvc.perform(post("/api/employee/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", equalTo("Already Registered")))
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void registerParticipant_capacityShouldUpdateImmediately() throws Exception {
        // Arrange
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(participantUser.getId(), testEvent.getId());

        // Act
        mockMvc.perform(post("/api/employee/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        // Assert
        Event updatedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(1, updatedEvent.getCurrentRegistrations());
        org.junit.jupiter.api.Assertions.assertEquals(EventStatus.OPEN, updatedEvent.getStatus());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "ADMIN")
    @Transactional
    void registerParticipant_withAdminRole_shouldSucceed() throws Exception {
        // Arrange
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(participantUser.getId(), testEvent.getId());

        // Act & Assert
        mockMvc.perform(post("/api/employee/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", equalTo("CONFIRMED")));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    @Transactional
    void registerParticipant_withNonEmployeeRole_shouldReturn403Forbidden() throws Exception {
        // Arrange
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(participantUser.getId(), testEvent.getId());

        // Act & Assert
        mockMvc.perform(post("/api/employee/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void registerParticipant_withInvalidUser_shouldReturn400BadRequest() throws Exception {
        // Arrange
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(999L, testEvent.getId());

        // Act & Assert
        mockMvc.perform(post("/api/employee/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    @Transactional
    void registerParticipant_withInvalidEvent_shouldReturn400BadRequest() throws Exception {
        // Arrange
        EmployeeRegistrationRequestModel request = new EmployeeRegistrationRequestModel(participantUser.getId(), 999L);

        // Act & Assert
        mockMvc.perform(post("/api/employee/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());
    }
}

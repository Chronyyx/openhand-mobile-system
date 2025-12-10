package com.mana.openhand_backend.registrations.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import com.mana.openhand_backend.registrations.domainclientlayer.RegistrationRequestModel;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class RegistrationControllerIntegrationTest {

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

        private User testUser;
        private Event testEvent;
        private LocalDateTime eventStart;
        private LocalDateTime eventEnd;

        @BeforeEach
        @Transactional
        void setUp() {
                eventStart = LocalDateTime.now().plusDays(1);
                eventEnd = LocalDateTime.now().plusDays(2);

                registrationRepository.deleteAll();
                eventRepository.deleteAll();
                userRepository.deleteAll();

                testUser = new User();
                testUser.setEmail("testuser@example.com");
                testUser.setPasswordHash("hashedPassword");
                testUser = userRepository.save(testUser);

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
                                "General");
                testEvent = eventRepository.save(testEvent);
        }

        // ========== registerForEvent Tests ==========

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void registerForEvent_withValidEvent_shouldReturn201Created() throws Exception {
                // Arrange
                RegistrationRequestModel request = new RegistrationRequestModel(testEvent.getId());

                // Act & Assert
                mockMvc.perform(post("/api/registrations")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id", notNullValue()))
                                .andExpect(jsonPath("$.userId", equalTo(testUser.getId().intValue())))
                                .andExpect(jsonPath("$.eventId", equalTo(testEvent.getId().intValue())))
                                .andExpect(jsonPath("$.status", equalTo("CONFIRMED")))
                                .andExpect(jsonPath("$.eventTitle", equalTo("Test Event")));
        }

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void registerForEvent_whenEventAtCapacity_shouldReturn201WithWaitlistedStatus() throws Exception {
                // Arrange - fill event to capacity
                Event filledEvent = eventRepository.findById(testEvent.getId()).get();
                filledEvent.setCurrentRegistrations(2);
                filledEvent.setStatus(EventStatus.FULL);
                eventRepository.save(filledEvent);

                RegistrationRequestModel request = new RegistrationRequestModel(testEvent.getId());

                // Act & Assert
                mockMvc.perform(post("/api/registrations")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status", equalTo("WAITLISTED")))
                                .andExpect(jsonPath("$.waitlistedPosition", equalTo(1)));
        }

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "ADMIN")
        @Transactional
        void registerForEvent_withNonMemberRole_shouldReturn403Forbidden() throws Exception {
                // Arrange
                RegistrationRequestModel request = new RegistrationRequestModel(testEvent.getId());

                // Act & Assert
                mockMvc.perform(post("/api/registrations")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        // ========== getMyRegistrations Tests ==========

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void getMyRegistrations_withValidUser_shouldReturnUserRegistrations() throws Exception {
                // Arrange
                Registration registration = new Registration(testUser, testEvent);
                registration.setStatus(RegistrationStatus.CONFIRMED);
                registration.setConfirmedAt(LocalDateTime.now());
                registrationRepository.save(registration);

                // Act & Assert
                mockMvc.perform(get("/api/registrations/my-registrations")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].userId", equalTo(testUser.getId().intValue())))
                                .andExpect(jsonPath("$[0].eventId", equalTo(testEvent.getId().intValue())))
                                .andExpect(jsonPath("$[0].status", equalTo("CONFIRMED")));
        }

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void getMyRegistrations_withNoRegistrations_shouldReturnEmptyList() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/api/registrations/my-registrations")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void getMyRegistrations_withMultipleRegistrations_shouldReturnAll() throws Exception {
                // Arrange
                Event event2 = new Event(
                                "Event 2",
                                "Description 2",
                                eventStart,
                                eventEnd,
                                "Location 2",
                                "Address 2",
                                EventStatus.OPEN,
                                10,
                                0,
                                "General");
                event2 = eventRepository.save(event2);

                Registration reg1 = new Registration(testUser, testEvent);
                reg1.setStatus(RegistrationStatus.CONFIRMED);
                registrationRepository.save(reg1);

                Registration reg2 = new Registration(testUser, event2);
                reg2.setStatus(RegistrationStatus.WAITLISTED);
                registrationRepository.save(reg2);

                // Act & Assert
                mockMvc.perform(get("/api/registrations/my-registrations")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].status", equalTo("CONFIRMED")))
                                .andExpect(jsonPath("$[1].status", equalTo("WAITLISTED")));
        }

        // ========== cancelRegistration Tests ==========

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void cancelRegistration_withValidRegistration_shouldReturnCancelledStatus() throws Exception {
                // Arrange
                Registration registration = new Registration(testUser, testEvent);
                registration.setStatus(RegistrationStatus.CONFIRMED);
                registration.setConfirmedAt(LocalDateTime.now());
                registrationRepository.save(registration);

                // Act & Assert
                mockMvc.perform(delete("/api/registrations/event/" + testEvent.getId())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", equalTo("CANCELLED")))
                                .andExpect(jsonPath("$.cancelledAt", notNullValue()));
        }

        // ========== Scenario Tests ==========

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void registrationWorkflow_registerThenCancel_shouldSucceed() throws Exception {
                // Arrange
                RegistrationRequestModel request = new RegistrationRequestModel(testEvent.getId());

                // Act 1
                mockMvc.perform(post("/api/registrations")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status", equalTo("CONFIRMED")));

                // Act 2
                mockMvc.perform(get("/api/registrations/my-registrations")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));

                // Act 3
                mockMvc.perform(delete("/api/registrations/event/" + testEvent.getId())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status", equalTo("CANCELLED")));

                // Act 4
                mockMvc.perform(get("/api/registrations/my-registrations")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].status", equalTo("CANCELLED")));
        }

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void registrationWorkflow_registerForFullEvent_shouldWaitlist() throws Exception {
                // Arrange
                Event filledEvent = eventRepository.findById(testEvent.getId()).get();
                filledEvent.setCurrentRegistrations(2);
                filledEvent.setStatus(EventStatus.FULL);
                eventRepository.save(filledEvent);

                RegistrationRequestModel request = new RegistrationRequestModel(testEvent.getId());

                // Act & Assert
                mockMvc.perform(post("/api/registrations")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status", equalTo("WAITLISTED")))
                                .andExpect(jsonPath("$.waitlistedPosition", greaterThan(0)));
        }

        @Test
        @WithMockUser(username = "testuser@example.com", roles = "MEMBER")
        @Transactional
        void registrationWorkflow_doubleRegister_shouldFail() throws Exception {
                // Arrange
                RegistrationRequestModel request = new RegistrationRequestModel(testEvent.getId());

                // Act 1
                mockMvc.perform(post("/api/registrations")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                // Act 2
                mockMvc.perform(post("/api/registrations")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict());
        }
}

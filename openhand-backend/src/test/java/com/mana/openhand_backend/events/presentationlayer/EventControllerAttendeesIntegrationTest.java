package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventControllerAttendeesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        testEvent = createAndSaveEvent("Attendees Test Event");
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void getEventAttendees_asEmployee_returnsAttendees() throws Exception {
        User user = createAndSaveUser("employee-attendee@example.com", "Employee Attendee", 29);
        createAndSaveRegistration(user, testEvent, RegistrationStatus.CONFIRMED);

        mockMvc.perform(get("/api/events/{id}/attendees", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(testEvent.getId()))
                .andExpect(jsonPath("$.totalAttendees").value(1))
                .andExpect(jsonPath("$.attendees", hasSize(1)))
                .andExpect(jsonPath("$.attendees[0].attendeeId").value(user.getId()))
                .andExpect(jsonPath("$.attendees[0].fullName").value("Employee Attendee"))
                .andExpect(jsonPath("$.attendees[0].age").value(29));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void getEventAttendees_asAdmin_returnsAttendees() throws Exception {
        User user = createAndSaveUser("admin-attendee@example.com", "Admin Attendee", 34);
        createAndSaveRegistration(user, testEvent, RegistrationStatus.CONFIRMED);

        mockMvc.perform(get("/api/events/{id}/attendees", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttendees").value(1))
                .andExpect(jsonPath("$.attendees[0].fullName").value("Admin Attendee"));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void getEventAttendees_asMember_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/events/{id}/attendees", testEvent.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void getEventAttendees_excludesCancelledRegistrations() throws Exception {
        User confirmedUser = createAndSaveUser("confirmed@example.com", "Confirmed User", 22);
        User cancelledUser = createAndSaveUser("cancelled@example.com", "Cancelled User", 41);

        createAndSaveRegistration(confirmedUser, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(cancelledUser, testEvent, RegistrationStatus.CANCELLED);

        mockMvc.perform(get("/api/events/{id}/attendees", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttendees").value(1))
                .andExpect(jsonPath("$.attendees", hasSize(1)))
                .andExpect(jsonPath("$.attendees[0].fullName").value("Confirmed User"));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void getEventAttendees_returnsCorrectTotalCount() throws Exception {
        User requestedUser = createAndSaveUser("requested@example.com", "Requested User", 19);
        User confirmedUser = createAndSaveUser("confirmed2@example.com", "Confirmed User Two", 26);
        User waitlistedUser = createAndSaveUser("waitlisted@example.com", "Waitlisted User", 31);

        createAndSaveRegistration(requestedUser, testEvent, RegistrationStatus.REQUESTED);
        createAndSaveRegistration(confirmedUser, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(waitlistedUser, testEvent, RegistrationStatus.WAITLISTED);

        mockMvc.perform(get("/api/events/{id}/attendees", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttendees").value(3))
                .andExpect(jsonPath("$.attendees", hasSize(3)));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void getEventAttendees_withNoRegistrations_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/events/{id}/attendees", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttendees").value(0))
                .andExpect(jsonPath("$.attendees", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void getEventAttendees_withInvalidEvent_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/events/{id}/attendees", 99999L))
                .andExpect(status().isNotFound());
    }

    private Event createAndSaveEvent(String title) {
        LocalDateTime start = LocalDateTime.of(2025, 6, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 15, 21, 0);
        Event event = new Event(
                title,
                "Attendees test",
                start,
                end,
                "Test Location",
                "123 Test St",
                EventStatus.OPEN,
                100,
                0,
                "Test"
        );
        return eventRepository.save(event);
    }

    private User createAndSaveUser(String email, String name, Integer age) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        user.setName(name);
        user.setAge(age);
        return userRepository.save(user);
    }

    private Registration createAndSaveRegistration(User user, Event event, RegistrationStatus status) {
        Registration registration = new Registration(user, event);
        registration.setStatus(status);
        if (status == RegistrationStatus.CONFIRMED) {
            registration.setConfirmedAt(LocalDateTime.now());
        }
        if (status == RegistrationStatus.CANCELLED) {
            registration.setCancelledAt(LocalDateTime.now());
        }
        return registrationRepository.save(registration);
    }
}

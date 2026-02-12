package com.mana.openhand_backend.attendance.presentationlayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreferenceRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(locations = "classpath:application-test.properties")
class AttendanceReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        registrationRepository.deleteAll();
        registrationRepository.flush();
        eventRepository.deleteAll();
        eventRepository.flush();
        notificationPreferenceRepository.deleteAll();
        notificationPreferenceRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void adminCanAccess_returnsPerEventRows() throws Exception {
        Event eventOne = createEvent("Gala", LocalDateTime.of(2026, 1, 12, 10, 0));
        Event eventTwo = createEvent("Workshop", LocalDateTime.of(2026, 1, 13, 14, 0));

        createRegistration(eventOne, true, RegistrationStatus.CONFIRMED);
        createRegistration(eventOne, false, RegistrationStatus.CONFIRMED);
        createRegistration(eventTwo, true, RegistrationStatus.CONFIRMED);

        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventId").value(eventOne.getId()))
                .andExpect(jsonPath("$[0].totalAttended").value(1))
                .andExpect(jsonPath("$[0].totalRegistered").value(2))
                .andExpect(jsonPath("$[1].eventId").value(eventTwo.getId()))
                .andExpect(jsonPath("$[1].totalAttended").value(1))
                .andExpect(jsonPath("$[1].totalRegistered").value(1));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void memberCannotAccess_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "employee@example.com", roles = "EMPLOYEE")
    void employeeCannotAccess_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedCannotAccess_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void dateRangeFilteringWorks() throws Exception {
        Event inRange = createEvent("In Range", LocalDateTime.of(2026, 2, 15, 9, 0));
        createEvent("Out Of Range", LocalDateTime.of(2026, 3, 1, 9, 0));
        createRegistration(inRange, true, RegistrationStatus.CONFIRMED);

        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-02-01")
                        .param("endDate", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventId").value(inRange.getId()))
                .andExpect(jsonPath("$[0].eventTitle").value("In Range"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void eventIdFilteringWorks() throws Exception {
        Event eventOne = createEvent("Target Event", LocalDateTime.of(2026, 1, 10, 10, 0));
        Event eventTwo = createEvent("Other Event", LocalDateTime.of(2026, 1, 11, 10, 0));
        createRegistration(eventOne, true, RegistrationStatus.CONFIRMED);
        createRegistration(eventTwo, true, RegistrationStatus.CONFIRMED);

        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .param("eventId", String.valueOf(eventOne.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventId").value(eventOne.getId()))
                .andExpect(jsonPath("$[0].eventTitle").value("Target Event"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void eventIdNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .param("eventId", "9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void startDateAfterEndDate_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-01-31")
                        .param("endDate", "2026-01-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void eventWithNoAttendance_returnsZeroAttended() throws Exception {
        Event event = createEvent("No Check-ins", LocalDateTime.of(2026, 4, 8, 10, 0));
        createRegistration(event, false, RegistrationStatus.CONFIRMED);

        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalAttended").value(0))
                .andExpect(jsonPath("$[0].totalRegistered").value(1))
                .andExpect(jsonPath("$[0].attendanceRate").value(0.0));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    @Transactional
    void dataReflectsCheckedInAttendance_notRegistrationsOnly() throws Exception {
        Event event = createEvent("Check-in Accuracy", LocalDateTime.of(2026, 5, 10, 12, 0));
        createRegistration(event, true, RegistrationStatus.CONFIRMED);
        createRegistration(event, false, RegistrationStatus.CONFIRMED);
        createRegistration(event, true, RegistrationStatus.CANCELLED);

        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalAttended").value(1))
                .andExpect(jsonPath("$[0].totalRegistered").value(2))
                .andExpect(jsonPath("$[0].attendanceRate").value(0.5));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void emptyRange_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/admin/attendance-reports")
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private Event createEvent(String title, LocalDateTime startDateTime) {
        Event event = new Event(
                title,
                "Report test event",
                startDateTime,
                startDateTime.plusHours(2),
                "MANA Center",
                "1910 Test Blvd",
                EventStatus.OPEN,
                100,
                0,
                "General"
        );
        return eventRepository.saveAndFlush(event);
    }

    private void createRegistration(Event event, boolean checkedIn, RegistrationStatus status) {
        User user = new User();
        user.setEmail("user-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hashedPassword");
        user.setRoles(new HashSet<>());
        User savedUser = userRepository.saveAndFlush(user);

        Registration registration = new Registration(savedUser, event);
        registration.setStatus(status);
        registration.setRequestedAt(LocalDateTime.now());
        if (status == RegistrationStatus.CONFIRMED) {
            registration.setConfirmedAt(LocalDateTime.now());
        }
        if (checkedIn) {
            registration.setCheckedInAt(LocalDateTime.now());
        }
        if (status == RegistrationStatus.CANCELLED) {
            registration.setCancelledAt(LocalDateTime.now());
        }
        registrationRepository.saveAndFlush(registration);
    }
}

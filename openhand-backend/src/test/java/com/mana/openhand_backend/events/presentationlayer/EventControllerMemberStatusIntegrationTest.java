package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EventControllerMemberStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    private Event testEvent;
    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        testEvent = new Event(
                "Member Status Test Event",
                "Testing member status visibility in registration summary",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Test Location",
                "Test Address",
                EventStatus.OPEN,
                10,
                2,
                "General");
        testEvent = eventRepository.save(testEvent);

        activeUser = new User("active@test.com", "hashedPassword123", new java.util.HashSet<>());
        activeUser.setName("Active Test User");
        activeUser.setMemberStatus(MemberStatus.ACTIVE);
        activeUser = userRepository.save(activeUser);

        inactiveUser = new User("inactive@test.com", "hashedPassword456", new java.util.HashSet<>());
        inactiveUser.setName("Inactive Test User");
        inactiveUser.setMemberStatus(MemberStatus.INACTIVE);
        inactiveUser = userRepository.save(inactiveUser);
    }

    @Test
    void getRegistrationSummary_returnsMemberStatusForAttendees() throws Exception {
        // arrange
        Registration confirmedReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now().minusDays(1));
        confirmedReg.setConfirmedAt(LocalDateTime.now().minusDays(1));
        registrationRepository.save(confirmedReg);

        // act & assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees[0].memberStatus", is("ACTIVE")));
    }

    @Test
    void getRegistrationSummary_includesWaitlistedAttendeesWithStatus() throws Exception {
        // arrange
        Registration waitlistedReg = new Registration(inactiveUser, testEvent, RegistrationStatus.WAITLISTED, LocalDateTime.now());
        waitlistedReg.setWaitlistedPosition(1);
        registrationRepository.save(waitlistedReg);

        // act & assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees[0].memberStatus", is("INACTIVE")))
                .andExpect(jsonPath("$.attendees[0].registrationStatus", is("WAITLISTED")));
    }

    @Test
    void getRegistrationSummary_displaysBothActiveAndInactiveMembers() throws Exception {
        // arrange
        Registration activeReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now().minusDays(2));
        activeReg.setConfirmedAt(LocalDateTime.now().minusDays(2));
        registrationRepository.save(activeReg);

        Registration inactiveReg = new Registration(inactiveUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now().minusDays(1));
        inactiveReg.setConfirmedAt(LocalDateTime.now().minusDays(1));
        registrationRepository.save(inactiveReg);

        // act & assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees[*].memberStatus", hasItems("ACTIVE", "INACTIVE")));
    }

    @Test
    void getRegistrationSummary_attendeeContainsCorrectUserInformation() throws Exception {
        // arrange
        Registration reg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        reg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(reg);

        // act & assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees[0].userName", is("Active Test User")))
                .andExpect(jsonPath("$.attendees[0].userEmail", is("active@test.com")))
                .andExpect(jsonPath("$.attendees[0].memberStatus", is("ACTIVE")));
    }

    @Test
    void getRegistrationSummary_excludesCancelledRegistrationsFromAttendeeList() throws Exception {
        // arrange
        Registration confirmedReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        confirmedReg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(confirmedReg);

        Registration cancelledReg = new Registration(inactiveUser, testEvent, RegistrationStatus.CANCELLED, LocalDateTime.now());
        registrationRepository.save(cancelledReg);

        // act & assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees", hasSize(1)))
                .andExpect(jsonPath("$.attendees[0].registrationStatus", is("CONFIRMED")));
    }

    @Test
    void getRegistrationSummary_includesTimestampsInAttendeeResponse() throws Exception {
        // arrange
        LocalDateTime requestedAt = LocalDateTime.now().minusDays(1);
        LocalDateTime confirmedAt = LocalDateTime.now();

        Registration reg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, requestedAt);
        reg.setConfirmedAt(confirmedAt);
        registrationRepository.save(reg);

        // act & assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees[0].requestedAt", notNullValue()))
                .andExpect(jsonPath("$.attendees[0].confirmedAt", notNullValue()));
    }

    @Test
    void getRegistrationSummary_emptyEventReturnsEmptyAttendeeList() throws Exception {
        // act & assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees", hasSize(0)));
    }

    @Test
    void getRegistrationSummary_ordersAttendeesConsistently() throws Exception {
        // arrange
        User user1 = new User("user1@test.com", "hashedPassword789", new java.util.HashSet<>());
        user1.setName("User One");
        user1.setMemberStatus(MemberStatus.ACTIVE);
        user1 = userRepository.save(user1);

        User user2 = new User("user2@test.com", "hashedPassword101", new java.util.HashSet<>());
        user2.setName("User Two");
        user2.setMemberStatus(MemberStatus.INACTIVE);
        user2 = userRepository.save(user2);

        Registration reg1 = new Registration(user1, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now().minusHours(2));
        reg1.setConfirmedAt(LocalDateTime.now().minusHours(2));
        registrationRepository.save(reg1);

        Registration reg2 = new Registration(user2, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now().minusHours(1));
        reg2.setConfirmedAt(LocalDateTime.now().minusHours(1));
        registrationRepository.save(reg2);

        // act & assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendees[*].memberStatus", hasItems("ACTIVE", "INACTIVE")));
    }
}

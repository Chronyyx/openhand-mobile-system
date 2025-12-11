package com.mana.openhand_backend.events.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class EventControllerRegistrationSummaryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        LocalDateTime start = LocalDateTime.of(2025, 6, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 15, 21, 0);
        testEvent = new Event(
                "Integration Test Event",
                "Testing registration summary",
                start,
                end,
                "Test Location",
                "123 Test St",
                EventStatus.OPEN,
                100,
                40,
                "Test"
        );
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void getRegistrationSummary_withValidEventId_returnsCorrectSummary() throws Exception {
        // arrange
        User user1 = createAndSaveUser("user1@test.com");
        User user2 = createAndSaveUser("user2@test.com");
        User user3 = createAndSaveUser("user3@test.com");

        createAndSaveRegistration(user1, testEvent, RegistrationStatus.WAITLISTED);
        createAndSaveRegistration(user2, testEvent, RegistrationStatus.WAITLISTED);
        createAndSaveRegistration(user3, testEvent, RegistrationStatus.WAITLISTED);

        // act + assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", testEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(testEvent.getId()))
                .andExpect(jsonPath("$.totalRegistrations").value(40))
                .andExpect(jsonPath("$.waitlistedCount").value(3))
                .andExpect(jsonPath("$.maxCapacity").value(100))
                .andExpect(jsonPath("$.remainingSpots").value(60))
                .andExpect(jsonPath("$.percentageFull").value(40.0));
    }

    @Test
    void getRegistrationSummary_withUnlimitedCapacity_returnsNullValues() throws Exception {
        // arrange
        LocalDateTime start = LocalDateTime.of(2025, 7, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 7, 15, 21, 0);
        Event unlimitedEvent = new Event(
                "Unlimited Event",
                "No capacity limit",
                start,
                end,
                "Test Location",
                "456 Test Ave",
                EventStatus.OPEN,
                null,
                25,
                "Test"
        );
        unlimitedEvent = eventRepository.save(unlimitedEvent);

        // act + assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", unlimitedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(unlimitedEvent.getId()))
                .andExpect(jsonPath("$.totalRegistrations").value(25))
                .andExpect(jsonPath("$.waitlistedCount").value(0))
                .andExpect(jsonPath("$.maxCapacity").doesNotExist())
                .andExpect(jsonPath("$.remainingSpots").doesNotExist())
                .andExpect(jsonPath("$.percentageFull").doesNotExist());
    }

    @Test
    void getRegistrationSummary_withNoRegistrations_returnsZeroCounts() throws Exception {
        // arrange
        LocalDateTime start = LocalDateTime.of(2025, 8, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 8, 15, 21, 0);
        Event emptyEvent = new Event(
                "Empty Event",
                "No registrations",
                start,
                end,
                "Test Location",
                "789 Test Blvd",
                EventStatus.OPEN,
                50,
                0,
                "Test"
        );
        emptyEvent = eventRepository.save(emptyEvent);

        // act + assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", emptyEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(emptyEvent.getId()))
                .andExpect(jsonPath("$.totalRegistrations").value(0))
                .andExpect(jsonPath("$.waitlistedCount").value(0))
                .andExpect(jsonPath("$.maxCapacity").value(50))
                .andExpect(jsonPath("$.remainingSpots").value(50))
                .andExpect(jsonPath("$.percentageFull").value(0.0));
    }

    @Test
    void getRegistrationSummary_withFullEvent_returnsZeroRemainingSpots() throws Exception {
        // arrange
        LocalDateTime start = LocalDateTime.of(2025, 9, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 9, 15, 21, 0);
        Event fullEvent = new Event(
                "Full Event",
                "At capacity",
                start,
                end,
                "Test Location",
                "321 Test Rd",
                EventStatus.FULL,
                50,
                50,
                "Test"
        );
        fullEvent = eventRepository.save(fullEvent);

        User user1 = createAndSaveUser("waitlisted1@test.com");
        User user2 = createAndSaveUser("waitlisted2@test.com");

        createAndSaveRegistration(user1, fullEvent, RegistrationStatus.WAITLISTED);
        createAndSaveRegistration(user2, fullEvent, RegistrationStatus.WAITLISTED);

        // act + assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", fullEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(fullEvent.getId()))
                .andExpect(jsonPath("$.totalRegistrations").value(50))
                .andExpect(jsonPath("$.waitlistedCount").value(2))
                .andExpect(jsonPath("$.maxCapacity").value(50))
                .andExpect(jsonPath("$.remainingSpots").value(0))
                .andExpect(jsonPath("$.percentageFull").value(100.0));
    }

    @Test
    void getRegistrationSummary_withInvalidEventId_returnsNotFound() throws Exception {
        // arrange
        Long invalidEventId = 99999L;

        // act + assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", invalidEventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRegistrationSummary_withNullCurrentRegistrations_fallsBackToDatabaseCount() throws Exception {
        // arrange
        LocalDateTime start = LocalDateTime.of(2025, 10, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 10, 15, 21, 0);
        Event legacyEvent = new Event(
                "Legacy Event",
                "Null current registrations",
                start,
                end,
                "Test Location",
                "654 Test Way",
                EventStatus.OPEN,
                75,
                null,
                "Test"
        );
        legacyEvent = eventRepository.save(legacyEvent);

        User user1 = createAndSaveUser("legacy1@test.com");
        User user2 = createAndSaveUser("legacy2@test.com");
        User user3 = createAndSaveUser("legacy3@test.com");
        User user4 = createAndSaveUser("legacy4@test.com");

        createAndSaveRegistration(user1, legacyEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user2, legacyEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user3, legacyEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user4, legacyEvent, RegistrationStatus.WAITLISTED);

        // act + assert
        mockMvc.perform(get("/api/events/{id}/registration-summary", legacyEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(legacyEvent.getId()))
                .andExpect(jsonPath("$.totalRegistrations").value(3))
                .andExpect(jsonPath("$.waitlistedCount").value(1))
                .andExpect(jsonPath("$.maxCapacity").value(75))
                .andExpect(jsonPath("$.remainingSpots").value(72))
                .andExpect(jsonPath("$.percentageFull").value(4.0));
    }

    private User createAndSaveUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        return userRepository.save(user);
    }

    private Registration createAndSaveRegistration(User user, Event event, RegistrationStatus status) {
        Registration registration = new Registration(user, event);
        registration.setStatus(status);
        if (status == RegistrationStatus.CONFIRMED) {
            registration.setConfirmedAt(LocalDateTime.now());
        }
        return registrationRepository.save(registration);
    }
}

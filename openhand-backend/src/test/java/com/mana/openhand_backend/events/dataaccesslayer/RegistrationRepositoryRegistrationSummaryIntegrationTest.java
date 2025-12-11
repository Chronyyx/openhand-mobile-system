package com.mana.openhand_backend.events.dataaccesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RegistrationRepositoryRegistrationSummaryIntegrationTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        LocalDateTime start = LocalDateTime.of(2025, 1, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 15, 21, 0);
        testEvent = new Event(
                "Test Event",
                "Test Description",
                start,
                end,
                "Test Location",
                "123 Test St",
                EventStatus.OPEN,
                100,
                0,
                "Test Category"
        );
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void countByEventIdAndStatus_withConfirmedRegistrations_returnsCorrectCount() {
        // arrange
        User user1 = createAndSaveUser("user1@test.com");
        User user2 = createAndSaveUser("user2@test.com");
        User user3 = createAndSaveUser("user3@test.com");

        createAndSaveRegistration(user1, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user2, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user3, testEvent, RegistrationStatus.WAITLISTED);

        entityManager.flush();

        // act
        long confirmedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CONFIRMED);

        // assert
        assertEquals(2, confirmedCount);
    }

    @Test
    void countByEventIdAndStatus_withWaitlistedRegistrations_returnsCorrectCount() {
        // arrange
        User user1 = createAndSaveUser("user1@test.com");
        User user2 = createAndSaveUser("user2@test.com");
        User user3 = createAndSaveUser("user3@test.com");
        User user4 = createAndSaveUser("user4@test.com");

        createAndSaveRegistration(user1, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user2, testEvent, RegistrationStatus.WAITLISTED);
        createAndSaveRegistration(user3, testEvent, RegistrationStatus.WAITLISTED);
        createAndSaveRegistration(user4, testEvent, RegistrationStatus.WAITLISTED);

        entityManager.flush();

        // act
        long waitlistedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.WAITLISTED);

        // assert
        assertEquals(3, waitlistedCount);
    }

    @Test
    void countByEventIdAndStatus_withNoRegistrations_returnsZero() {
        // arrange

        // act
        long confirmedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CONFIRMED);
        long waitlistedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.WAITLISTED);

        // assert
        assertEquals(0, confirmedCount);
        assertEquals(0, waitlistedCount);
    }

    @Test
    void countByEventIdAndStatus_excludesCancelledRegistrations() {
        // arrange
        User user1 = createAndSaveUser("user1@test.com");
        User user2 = createAndSaveUser("user2@test.com");
        User user3 = createAndSaveUser("user3@test.com");

        createAndSaveRegistration(user1, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user2, testEvent, RegistrationStatus.CONFIRMED);

        Registration cancelledReg = createAndSaveRegistration(user3, testEvent, RegistrationStatus.CANCELLED);
        cancelledReg.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(cancelledReg);

        entityManager.flush();

        // act
        long confirmedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CONFIRMED);

        // assert
        assertEquals(2, confirmedCount);
    }

    @Test
    void countByEventIdAndStatus_withMixedStatuses_returnsCorrectCountsPerStatus() {
        // arrange
        User user1 = createAndSaveUser("user1@test.com");
        User user2 = createAndSaveUser("user2@test.com");
        User user3 = createAndSaveUser("user3@test.com");
        User user4 = createAndSaveUser("user4@test.com");
        User user5 = createAndSaveUser("user5@test.com");

        createAndSaveRegistration(user1, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user2, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user3, testEvent, RegistrationStatus.CONFIRMED);
        createAndSaveRegistration(user4, testEvent, RegistrationStatus.WAITLISTED);
        createAndSaveRegistration(user5, testEvent, RegistrationStatus.CANCELLED);

        entityManager.flush();

        // act
        long confirmedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CONFIRMED);
        long waitlistedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.WAITLISTED);
        long cancelledCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CANCELLED);

        // assert
        assertEquals(3, confirmedCount);
        assertEquals(1, waitlistedCount);
        assertEquals(1, cancelledCount);
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

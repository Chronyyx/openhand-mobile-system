package com.mana.openhand_backend.registrations.dataaccesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RegistrationRepositoryIntegrationTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("testuser@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser = userRepository.save(testUser);

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
                50
        );
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void findByUserIdAndEventId_withValidIds_returnsRegistration() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(registration);
        entityManager.flush();

        // Act
        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(testUser.getId(), testEvent.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getUser().getId());
        assertEquals(testEvent.getId(), result.get().getEvent().getId());
        assertEquals(RegistrationStatus.CONFIRMED, result.get().getStatus());
    }

    @Test
    void findByUserIdAndEventId_withInvalidIds_returnsEmpty() {
        // Arrange

        // Act
        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(999L, 999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUserIdAndEventId_whenRegistrationExists_returnsTrue() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registrationRepository.save(registration);
        entityManager.flush();

        // Act
        boolean exists = registrationRepository.existsByUserIdAndEventId(testUser.getId(), testEvent.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    void existsByUserIdAndEventId_whenRegistrationDoesNotExist_returnsFalse() {
        // Arrange

        // Act
        boolean exists = registrationRepository.existsByUserIdAndEventId(999L, 999L);

        // Assert
        assertFalse(exists);
    }

    @Test
    void existsByUserIdAndEventId_withCancelledRegistration_returnsTrue() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(registration);
        entityManager.flush();

        // Act
        boolean exists = registrationRepository.existsByUserIdAndEventId(testUser.getId(), testEvent.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    void countByEventIdAndStatus_confirmedRegistrations_returnsCorrectCount() {
        // Arrange
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hashedPassword");
        user2 = userRepository.save(user2);

        User user3 = new User();
        user3.setEmail("user3@example.com");
        user3.setPasswordHash("hashedPassword");
        user3 = userRepository.save(user3);

        Registration reg1 = new Registration(testUser, testEvent);
        reg1.setStatus(RegistrationStatus.CONFIRMED);
        reg1.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(reg1);

        Registration reg2 = new Registration(user2, testEvent);
        reg2.setStatus(RegistrationStatus.CONFIRMED);
        reg2.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(reg2);

        Registration reg3 = new Registration(user3, testEvent);
        reg3.setStatus(RegistrationStatus.WAITLISTED);
        reg3.setWaitlistedPosition(1);
        registrationRepository.save(reg3);

        entityManager.flush();

        // Act
        long count = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CONFIRMED);

        // Assert
        assertEquals(2L, count);
    }

    @Test
    void countByEventIdAndStatus_waitlistedRegistrations_returnsCorrectCount() {
        // Arrange
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hashedPassword");
        user2 = userRepository.save(user2);

        Registration reg1 = new Registration(testUser, testEvent);
        reg1.setStatus(RegistrationStatus.WAITLISTED);
        reg1.setWaitlistedPosition(1);
        registrationRepository.save(reg1);

        Registration reg2 = new Registration(user2, testEvent);
        reg2.setStatus(RegistrationStatus.WAITLISTED);
        reg2.setWaitlistedPosition(2);
        registrationRepository.save(reg2);

        entityManager.flush();

        // Act
        long count = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.WAITLISTED);

        // Assert
        assertEquals(2L, count);
    }

    @Test
    void countByEventIdAndStatus_noRegistrations_returnsZero() {
        // Arrange

        // Act
        long count = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CONFIRMED);

        // Assert
        assertEquals(0L, count);
    }

    @Test
    void countByEventIdAndStatus_mixedStatuses_countsOnlySpecifiedStatus() {
        // Arrange
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hashedPassword");
        user2 = userRepository.save(user2);

        User user3 = new User();
        user3.setEmail("user3@example.com");
        user3.setPasswordHash("hashedPassword");
        user3 = userRepository.save(user3);

        Registration reg1 = new Registration(testUser, testEvent);
        reg1.setStatus(RegistrationStatus.CONFIRMED);
        reg1.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(reg1);

        Registration reg2 = new Registration(user2, testEvent);
        reg2.setStatus(RegistrationStatus.CONFIRMED);
        reg2.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(reg2);

        Registration reg3 = new Registration(user3, testEvent);
        reg3.setStatus(RegistrationStatus.WAITLISTED);
        reg3.setWaitlistedPosition(1);
        registrationRepository.save(reg3);

        entityManager.flush();

        // Act
        long confirmedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CONFIRMED);
        long waitlistedCount = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.WAITLISTED);

        // Assert
        assertEquals(2L, confirmedCount);
        assertEquals(1L, waitlistedCount);
    }

    @Test
    void findByUserIdAndEventId_afterUpdate_returnsUpdatedRegistration() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setConfirmedAt(LocalDateTime.now());
        registration = registrationRepository.save(registration);
        entityManager.flush();

        // Act
        registration.setStatus(RegistrationStatus.WAITLISTED);
        registration.setWaitlistedPosition(1);
        registrationRepository.save(registration);
        entityManager.flush();

        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(testUser.getId(), testEvent.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(RegistrationStatus.WAITLISTED, result.get().getStatus());
        assertEquals(1, result.get().getWaitlistedPosition());
    }

    @Test
    void findByUserIdAndEventId_afterCancellation_returnsCancelledRegistration() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setConfirmedAt(LocalDateTime.now());
        registration = registrationRepository.save(registration);
        entityManager.flush();

        // Act
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(registration);
        entityManager.flush();

        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(testUser.getId(), testEvent.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(RegistrationStatus.CANCELLED, result.get().getStatus());
        assertNotNull(result.get().getCancelledAt());
    }

    @Test
    void save_newRegistration_persistsCorrectly() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setConfirmedAt(LocalDateTime.now());

        // Act
        Registration saved = registrationRepository.save(registration);
        entityManager.flush();

        // Assert
        assertNotNull(saved.getId());
        assertEquals(testUser.getId(), saved.getUser().getId());
        assertEquals(testEvent.getId(), saved.getEvent().getId());
    }

    @Test
    void countByEventIdAndStatus_cancelledRegistrations_returnsCorrectCount() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(registration);
        entityManager.flush();

        // Act
        long count = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CANCELLED);

        // Assert
        assertEquals(1L, count);
    }

    @Test
    void findByUserIdAndEventId_withNullUserId_returnsEmpty() {
        // Arrange

        // Act
        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(null, testEvent.getId());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdAndEventId_withNullEventId_returnsEmpty() {
        // Arrange

        // Act
        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(testUser.getId(), null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUserIdAndEventId_withNullUserId_returnsFalse() {
        // Arrange

        // Act
        boolean exists = registrationRepository.existsByUserIdAndEventId(null, testEvent.getId());

        // Assert
        assertFalse(exists);
    }

    @Test
    void existsByUserIdAndEventId_withNullEventId_returnsFalse() {
        // Arrange

        // Act
        boolean exists = registrationRepository.existsByUserIdAndEventId(testUser.getId(), null);

        // Assert
        assertFalse(exists);
    }

    @Test
    void countByEventIdAndStatus_withNullEventId_returnsZero() {
        // Arrange

        // Act
        long count = registrationRepository.countByEventIdAndStatus(null, RegistrationStatus.CONFIRMED);

        // Assert
        assertEquals(0L, count);
    }

    @Test
    void countByEventIdAndStatus_withNullStatus_returnsZero() {
        // Arrange

        // Act
        long count = registrationRepository.countByEventIdAndStatus(testEvent.getId(), null);

        // Assert
        assertEquals(0L, count);
    }

    @Test
    void deleteAll_withMultipleRegistrations_deletesAll() {
        // Arrange
        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hashedPassword");
        user2 = userRepository.save(user2);

        Registration reg1 = new Registration(testUser, testEvent);
        reg1.setStatus(RegistrationStatus.CONFIRMED);
        registrationRepository.save(reg1);

        Registration reg2 = new Registration(user2, testEvent);
        reg2.setStatus(RegistrationStatus.WAITLISTED);
        registrationRepository.save(reg2);

        entityManager.flush();

        // Act
        registrationRepository.deleteAll();
        entityManager.flush();

        // Assert
        assertEquals(0L, registrationRepository.count());
    }

    @Test
    void registration_withWaitlistedPosition_preservesPosition() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.WAITLISTED);
        registration.setWaitlistedPosition(5);
        registration = registrationRepository.save(registration);
        entityManager.flush();

        // Act
        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(testUser.getId(), testEvent.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(5, result.get().getWaitlistedPosition());
    }

    @Test
    void registration_withNullWaitlistedPosition_staysNull() {
        // Arrange
        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setWaitlistedPosition(null);
        registration = registrationRepository.save(registration);
        entityManager.flush();

        // Act
        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(testUser.getId(), testEvent.getId());

        // Assert
        assertTrue(result.isPresent());
        assertNull(result.get().getWaitlistedPosition());
    }

    @Test
    void countByEventIdAndStatus_acrossMultipleEvents_countsOnlySpecifiedEvent() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 2, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2025, 2, 15, 21, 0);
        Event event2 = new Event(
                "Event 2",
                "Description 2",
                start,
                end,
                "Location 2",
                "Address 2",
                EventStatus.OPEN,
                100,
                50
        );
        event2 = eventRepository.save(event2);

        Registration reg1 = new Registration(testUser, testEvent);
        reg1.setStatus(RegistrationStatus.CONFIRMED);
        registrationRepository.save(reg1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setPasswordHash("hashedPassword");
        user2 = userRepository.save(user2);

        Registration reg2 = new Registration(user2, event2);
        reg2.setStatus(RegistrationStatus.CONFIRMED);
        registrationRepository.save(reg2);

        entityManager.flush();

        // Act
        long event1Count = registrationRepository.countByEventIdAndStatus(testEvent.getId(), RegistrationStatus.CONFIRMED);
        long event2Count = registrationRepository.countByEventIdAndStatus(event2.getId(), RegistrationStatus.CONFIRMED);

        // Assert
        assertEquals(1L, event1Count);
        assertEquals(1L, event2Count);
    }

    @Test
    void registration_preservesDates_afterMultipleUpdates() {
        // Arrange
        LocalDateTime requestedAt = LocalDateTime.now();
        LocalDateTime confirmedAt = requestedAt.plusHours(1);
        LocalDateTime cancelledAt = requestedAt.plusDays(1);

        Registration registration = new Registration(testUser, testEvent);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setRequestedAt(requestedAt);
        registration.setConfirmedAt(confirmedAt);
        registration = registrationRepository.save(registration);
        entityManager.flush();

        // Act
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(cancelledAt);
        registrationRepository.save(registration);
        entityManager.flush();

        Optional<Registration> result = registrationRepository.findByUserIdAndEventId(testUser.getId(), testEvent.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(requestedAt, result.get().getRequestedAt());
        assertEquals(confirmedAt, result.get().getConfirmedAt());
        assertEquals(cancelledAt, result.get().getCancelledAt());
    }

    @Test
    void registration_uniqueConstraint_preventsDuplicates() {
        // Arrange
        Registration reg1 = new Registration(testUser, testEvent);
        reg1.setStatus(RegistrationStatus.CONFIRMED);
        registrationRepository.save(reg1);
        entityManager.flush();

        Registration reg2 = new Registration(testUser, testEvent);
        reg2.setStatus(RegistrationStatus.CONFIRMED);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            registrationRepository.save(reg2);
            entityManager.flush();
        });
    }
}

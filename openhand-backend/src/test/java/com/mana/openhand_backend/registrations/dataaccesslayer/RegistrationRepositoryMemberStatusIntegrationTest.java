package com.mana.openhand_backend.registrations.dataaccesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RegistrationRepositoryMemberStatusIntegrationTest {

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Event testEvent;
    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        testEvent = new Event(
                "Test Event",
                "Test Description",
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
        activeUser.setName("Active User");
        activeUser.setMemberStatus(MemberStatus.ACTIVE);
        activeUser = userRepository.save(activeUser);

        inactiveUser = new User("inactive@test.com", "hashedPassword456", new java.util.HashSet<>());
        inactiveUser.setName("Inactive User");
        inactiveUser.setMemberStatus(MemberStatus.INACTIVE);
        inactiveUser = userRepository.save(inactiveUser);

        entityManager.flush();
    }

    @Test
    void findByEventIdAndStatusIn_withConfirmedStatus_returnsOnlyConfirmedRegistrations() {
        // arrange
        Registration confirmedReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        confirmedReg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(confirmedReg);

        Registration cancelledReg = new Registration(inactiveUser, testEvent, RegistrationStatus.CANCELLED, LocalDateTime.now());
        registrationRepository.save(cancelledReg);

        entityManager.flush();

        // act
        List<Registration> result = registrationRepository.findByEventIdAndStatusIn(
                testEvent.getId(),
                Arrays.asList(RegistrationStatus.CONFIRMED)
        );

        // assert
        assertEquals(1, result.size());
        assertEquals(RegistrationStatus.CONFIRMED, result.get(0).getStatus());
    }

    @Test
    void findByEventIdAndStatusIn_withWaitlistedStatus_returnsOnlyWaitlistedRegistrations() {
        // arrange
        Registration confirmedReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        confirmedReg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(confirmedReg);

        Registration waitlistedReg = new Registration(inactiveUser, testEvent, RegistrationStatus.WAITLISTED, LocalDateTime.now());
        waitlistedReg.setWaitlistedPosition(1);
        registrationRepository.save(waitlistedReg);

        entityManager.flush();

        // act
        List<Registration> result = registrationRepository.findByEventIdAndStatusIn(
                testEvent.getId(),
                Arrays.asList(RegistrationStatus.WAITLISTED)
        );

        // assert
        assertEquals(1, result.size());
        assertEquals(RegistrationStatus.WAITLISTED, result.get(0).getStatus());
    }

    @Test
    void findByEventIdAndStatusIn_withMultipleStatuses_returnsAllMatchingRegistrations() {
        // arrange
        Registration confirmedReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        confirmedReg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(confirmedReg);

        Registration waitlistedReg = new Registration(inactiveUser, testEvent, RegistrationStatus.WAITLISTED, LocalDateTime.now());
        waitlistedReg.setWaitlistedPosition(1);
        registrationRepository.save(waitlistedReg);

        entityManager.flush();

        // act
        List<Registration> result = registrationRepository.findByEventIdAndStatusIn(
                testEvent.getId(),
                Arrays.asList(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED)
        );

        // assert
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(r -> r.getStatus() == RegistrationStatus.CONFIRMED));
        assertTrue(result.stream().anyMatch(r -> r.getStatus() == RegistrationStatus.WAITLISTED));
    }

    @Test
    void findByEventIdAndStatusIn_withNonMatchingEventId_returnsEmptyList() {
        // arrange
        Registration confirmedReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        confirmedReg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(confirmedReg);

        entityManager.flush();

        // act
        List<Registration> result = registrationRepository.findByEventIdAndStatusIn(
                999L,
                Arrays.asList(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED)
        );

        // assert
        assertEquals(0, result.size());
    }

    @Test
    void findByEventIdAndStatusIn_withNoCancelledStatus_excludesCancelledRegistrations() {
        // arrange
        Registration confirmedReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        confirmedReg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(confirmedReg);

        Registration cancelledReg = new Registration(inactiveUser, testEvent, RegistrationStatus.CANCELLED, LocalDateTime.now());
        registrationRepository.save(cancelledReg);

        entityManager.flush();

        // act
        List<Registration> result = registrationRepository.findByEventIdAndStatusIn(
                testEvent.getId(),
                Arrays.asList(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED)
        );

        // assert
        assertEquals(1, result.size());
        assertEquals(RegistrationStatus.CONFIRMED, result.get(0).getStatus());
    }

    @Test
    void findByEventIdAndStatusIn_preservesMemberStatusInformation() {
        // arrange
        Registration activeUserReg = new Registration(activeUser, testEvent, RegistrationStatus.CONFIRMED, LocalDateTime.now());
        activeUserReg.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(activeUserReg);

        Registration inactiveUserReg = new Registration(inactiveUser, testEvent, RegistrationStatus.WAITLISTED, LocalDateTime.now());
        inactiveUserReg.setWaitlistedPosition(1);
        registrationRepository.save(inactiveUserReg);

        entityManager.flush();

        // act
        List<Registration> result = registrationRepository.findByEventIdAndStatusIn(
                testEvent.getId(),
                Arrays.asList(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED)
        );

        // assert
        assertEquals(2, result.size());
        Registration activeReg = result.stream()
                .filter(r -> r.getUser().getEmail().equals(activeUser.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(activeReg);
        assertEquals(MemberStatus.ACTIVE, activeReg.getUser().getMemberStatus());

        Registration inactiveReg = result.stream()
                .filter(r -> r.getUser().getEmail().equals(inactiveUser.getEmail()))
                .findFirst()
                .orElse(null);
        assertNotNull(inactiveReg);
        assertEquals(MemberStatus.INACTIVE, inactiveReg.getUser().getMemberStatus());
    }
}

package com.mana.openhand_backend.registrations.presentationlayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.registrations.businesslayer.RegistrationService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
class RegistrationServiceIdempotencyTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Event event;

    @BeforeEach
    void setup() {
        user = new User();
        user.setEmail("idempotency@example.com");
        user.setPasswordHash("pw");
        user = userRepository.save(user);

        event = new Event(
                "Idempotency Event",
                "",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "Loc",
                "",
                EventStatus.OPEN,
                10,
                1,
                "General");
        event = eventRepository.save(event);

        Registration registration = new Registration(user, event);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setConfirmedAt(LocalDateTime.now());
        registrationRepository.save(registration);
    }

    @Test
    void cancelRegistration_shouldBeIdempotent() {
        // Act 1: First Cancellation
        registrationService.cancelRegistration(user.getId(), event.getId());

        // Reload from DB to ensure we have the persisted timestamp (handling DB
        // precision truncation)
        Registration cancelled1 = registrationRepository.findByUserIdAndEventId(user.getId(), event.getId())
                .orElseThrow();
        LocalDateTime firstCancelledAt = cancelled1.getCancelledAt();

        if (cancelled1.getStatus() != RegistrationStatus.CANCELLED) {
            assertEquals(RegistrationStatus.CANCELLED, cancelled1.getStatus(),
                    "DB START FAIL: Expected initial cancellation to set status to CANCELLED");
        }

        // Act 2: Second Cancellation
        Registration cancelled2 = registrationService.cancelRegistration(user.getId(), event.getId());
        LocalDateTime secondCancelledAt = cancelled2.getCancelledAt();

        // Assert
        assertEquals(RegistrationStatus.CANCELLED, cancelled2.getStatus());

        assertEquals(firstCancelledAt, secondCancelledAt,
                "CancelledAt timestamp should not change on second cancellation");
    }
}

package com.mana.openhand_backend.registrations.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.businesslayer.SendGridEmailService;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@org.springframework.test.context.TestPropertySource(properties = {
        "openhand.app.jwtSecret=testSecret",
        "openhand.app.jwtExpirationMs=3600000",
        "openhand.app.jwtRefreshExpirationMs=86400000"
})
public class RegistrationServiceImplWaitlistIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private SendGridEmailService sendGridEmailService;

    private Event event;
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    public void setup() {
        // Create an event with capacity 1
        event = new Event(
                "Limited Capacity Event",
                "Testing waitlist",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                "Test Loc",
                "123 Test St",
                EventStatus.OPEN,
                1,
                0,
                null);
        event = eventRepository.save(event);

        // Create users
        user1 = createUser("user1@example.com", "User One");
        user2 = createUser("user2@example.com", "User Two");
        user3 = createUser("user3@example.com", "User Three");
    }

    private User createUser(String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPasswordHash("password");
        user.setPreferredLanguage("en");
        return userRepository.save(user);
    }

    @Test
    public void testAutomaticPromotionWhenSpotOpens() {
        // 1. Register User 1 (Takes the only spot)
        registrationService.registerForEvent(user1.getId(), event.getId());

        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(1, updatedEvent.getCurrentRegistrations());
        assertEquals(EventStatus.FULL, updatedEvent.getStatus());

        // 2. Register User 2 (Goes to Waitlist #1)
        registrationService.registerForEvent(user2.getId(), event.getId());

        Registration reg2 = registrationRepository.findByUserIdAndEventId(user2.getId(), event.getId()).orElseThrow();
        assertEquals(RegistrationStatus.WAITLISTED, reg2.getStatus());
        assertEquals(1, reg2.getWaitlistedPosition());

        // 3. Register User 3 (Goes to Waitlist #2)
        registrationService.registerForEvent(user3.getId(), event.getId());

        Registration reg3 = registrationRepository.findByUserIdAndEventId(user3.getId(), event.getId()).orElseThrow();
        assertEquals(RegistrationStatus.WAITLISTED, reg3.getStatus());
        assertEquals(2, reg3.getWaitlistedPosition());

        // 4. User 1 Cancels
        registrationService.cancelRegistration(user1.getId(), event.getId());

        // 5. Verify User 2 is promoted
        Registration reg2Promoted = registrationRepository.findByUserIdAndEventId(user2.getId(), event.getId())
                .orElseThrow();
        assertEquals(RegistrationStatus.CONFIRMED, reg2Promoted.getStatus());
        assertNull(reg2Promoted.getWaitlistedPosition());

        // Notification sent to User 2
        verify(notificationService).createNotification(eq(user2.getId()), eq(event.getId()),
                eq("REGISTRATION_CONFIRMATION"), anyString());
        verify(sendGridEmailService).sendRegistrationConfirmation(eq(user2.getEmail()), anyString(), anyString(),
                anyString(), anyList());

        // 6. Verify User 3 is still waitlisted (Waitlist position logic is static in
        // this impl,
        // normally we might want to shift them up, but for now just checking they
        // didn't get promoted incorrectly)
        Registration reg3StillWaitlisted = registrationRepository.findByUserIdAndEventId(user3.getId(), event.getId())
                .orElseThrow();
        assertEquals(RegistrationStatus.WAITLISTED, reg3StillWaitlisted.getStatus());

        // 7. Verify Event is still full
        Event finalEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(1, finalEvent.getCurrentRegistrations());
        assertEquals(EventStatus.FULL, finalEvent.getStatus());
    }

    @Test
    public void testNoPromotionIfEventNotFullCheck() {
        // Only 1 user registers, then cancels. No one on waitlist.
        registrationService.registerForEvent(user1.getId(), event.getId());
        registrationService.cancelRegistration(user1.getId(), event.getId());

        Event finalEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(0, finalEvent.getCurrentRegistrations());
        assertEquals(EventStatus.OPEN, finalEvent.getStatus());
    }

    @Test
    public void testMultipleSpotsOpening() {
        // Increase capacity to 2 to test group cancellation logic potentially opening
        // multiple spots
        // Or simpler: Event capacity 2.
        // U1, U2 registered. U3 waitlisted.
        // U1 cancels. U3 promoted.

        event.setMaxCapacity(2);
        eventRepository.save(event);

        registrationService.registerForEvent(user1.getId(), event.getId());
        registrationService.registerForEvent(user2.getId(), event.getId());
        assertEquals(EventStatus.FULL, eventRepository.findById(event.getId()).get().getStatus());

        registrationService.registerForEvent(user3.getId(), event.getId()); // Waitlisted #1

        registrationService.cancelRegistration(user1.getId(), event.getId());

        Registration reg3 = registrationRepository.findByUserIdAndEventId(user3.getId(), event.getId()).orElseThrow();
        assertEquals(RegistrationStatus.CONFIRMED, reg3.getStatus());

        Event finalEvent = eventRepository.findById(event.getId()).orElseThrow();
        assertEquals(2, finalEvent.getCurrentRegistrations());
    }
}

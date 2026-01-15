package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceItemRequestModel;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceUpdateRequestModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class NotificationPreferenceEnforcementIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationPreferenceService preferenceService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    @Transactional
    void setUp() {
        notificationRepository.deleteAll();
        eventRepository.deleteAll();

        testUser = userRepository.findByEmail("notification_pref_enforce@example.com").orElseGet(() -> {
            User newUser = new User("notification_pref_enforce@example.com", "password123", Set.of("ROLE_MEMBER"));
            newUser.setPreferredLanguage("en");
            return userRepository.save(newUser);
        });

        testEvent = new Event(
                "Integration Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(3),
                "Test Location",
                "123 Test St",
                EventStatus.OPEN,
                100,
                0,
                "Conference"
        );
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    @Transactional
    void createNotification_disabledCategory_skipsSaving() {
        NotificationPreferenceUpdateRequestModel request = new NotificationPreferenceUpdateRequestModel(
                List.of(new NotificationPreferenceItemRequestModel("REMINDER", false))
        );
        preferenceService.updatePreferences(testUser.getId(), request);

        Notification result = notificationService.createNotification(
                testUser.getId(),
                testEvent.getId(),
                "REMINDER",
                "en"
        );

        assertNull(result);
        assertEquals(0, notificationRepository.count());
    }
}

package com.mana.openhand_backend.notifications.dataaccesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
class NotificationRepositoryIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    private User testUser1;
    private User testUser2;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        testUser1 = new User("user1@example.com", "password123", Set.of("ROLE_MEMBER"));
        testUser1.setPreferredLanguage("en");
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User("user2@example.com", "password456", Set.of("ROLE_MEMBER"));
        testUser2.setPreferredLanguage("fr");
        testUser2 = userRepository.save(testUser2);

        testEvent = new Event(
                "Test Event",
                "Test Description",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
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
    void findByUserIdOrderByCreatedAtDesc_multipleNotifications_returnsOrderedByNewestFirst() {
        // Arrange
        Notification notification1 = new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "You are confirmed", "Test Event"
        );
        notification1.setCreatedAt(LocalDateTime.now().minusHours(2));
        notificationRepository.save(notification1);

        Notification notification2 = new Notification(
                testUser1, testEvent, NotificationType.REMINDER,
                "en", "Reminder text", "Test Event"
        );
        notification2.setCreatedAt(LocalDateTime.now().minusHours(1));
        notificationRepository.save(notification2);

        Notification notification3 = new Notification(
                testUser1, testEvent, NotificationType.CANCELLATION,
                "en", "Cancelled", "Test Event"
        );
        notification3.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification3);

        // Act
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser1.getId());

        // Assert
        assertEquals(3, result.size());
        assertEquals(NotificationType.CANCELLATION, result.get(0).getNotificationType());
        assertEquals(NotificationType.REMINDER, result.get(1).getNotificationType());
        assertEquals(NotificationType.REGISTRATION_CONFIRMATION, result.get(2).getNotificationType());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_noNotifications_returnsEmptyList() {
        // Arrange
        Long nonExistentUserId = 999L;

        // Act
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(nonExistentUserId);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdOrderByCreatedAtDesc_onlyReturnsUserSpecificNotifications() {
        // Arrange
        notificationRepository.save(new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "User1 notification", "Test Event"
        ));
        notificationRepository.save(new Notification(
                testUser2, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "fr", "User2 notification", "Test Event"
        ));

        // Act
        List<Notification> user1Notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser1.getId());
        List<Notification> user2Notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser2.getId());

        // Assert
        assertEquals(1, user1Notifications.size());
        assertEquals(1, user2Notifications.size());
        assertEquals("en", user1Notifications.get(0).getLanguage());
        assertEquals("fr", user2Notifications.get(0).getLanguage());
    }

    @Test
    void countByUserIdAndIsReadFalse_mixedReadUnreadNotifications_countsOnlyUnread() {
        // Arrange
        Notification unread1 = new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Unread 1", "Test Event"
        );
        notificationRepository.save(unread1);

        Notification unread2 = new Notification(
                testUser1, testEvent, NotificationType.REMINDER,
                "en", "Unread 2", "Test Event"
        );
        notificationRepository.save(unread2);

        Notification read = new Notification(
                testUser1, testEvent, NotificationType.CANCELLATION,
                "en", "Read", "Test Event"
        );
        read.setRead(true);
        read.setReadAt(LocalDateTime.now());
        notificationRepository.save(read);

        // Act
        long count = notificationRepository.countByUserIdAndIsReadFalse(testUser1.getId());

        // Assert
        assertEquals(2, count);
    }

    @Test
    void countByUserIdAndIsReadFalse_allNotificationsRead_returnsZero() {
        // Arrange
        Notification notification = new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Read notification", "Test Event"
        );
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        // Act
        long count = notificationRepository.countByUserIdAndIsReadFalse(testUser1.getId());

        // Assert
        assertEquals(0, count);
    }

    @Test
    void countByUserIdAndIsReadFalse_noNotifications_returnsZero() {
        // Arrange
        Long nonExistentUserId = 999L;

        // Act
        long count = notificationRepository.countByUserIdAndIsReadFalse(nonExistentUserId);

        // Assert
        assertEquals(0, count);
    }

    @Test
    void findByEventIdAndNotificationType_multipleMatchingNotifications_returnsAll() {
        // Arrange
        notificationRepository.save(new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "User1 confirmation", "Test Event"
        ));
        notificationRepository.save(new Notification(
                testUser2, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "fr", "User2 confirmation", "Test Event"
        ));
        notificationRepository.save(new Notification(
                testUser1, testEvent, NotificationType.REMINDER,
                "en", "User1 reminder", "Test Event"
        ));

        // Act
        List<Notification> confirmations = notificationRepository.findByEventIdAndNotificationType(
                testEvent.getId(), NotificationType.REGISTRATION_CONFIRMATION
        );
        List<Notification> reminders = notificationRepository.findByEventIdAndNotificationType(
                testEvent.getId(), NotificationType.REMINDER
        );

        // Assert
        assertEquals(2, confirmations.size());
        assertEquals(1, reminders.size());
    }

    @Test
    void findByEventIdAndNotificationType_noMatchingNotifications_returnsEmptyList() {
        // Arrange
        notificationRepository.save(new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Confirmation", "Test Event"
        ));

        // Act
        List<Notification> result = notificationRepository.findByEventIdAndNotificationType(
                testEvent.getId(), NotificationType.CANCELLATION
        );

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdAndIsReadFalse_onlyReturnsUnreadNotifications() {
        // Arrange
        Notification unread = new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Unread", "Test Event"
        );
        notificationRepository.save(unread);

        Notification read = new Notification(
                testUser1, testEvent, NotificationType.REMINDER,
                "en", "Read", "Test Event"
        );
        read.setRead(true);
        read.setReadAt(LocalDateTime.now());
        notificationRepository.save(read);

        // Act
        List<Notification> result = notificationRepository.findByUserIdAndIsReadFalse(testUser1.getId());

        // Assert
        assertEquals(1, result.size());
        assertFalse(result.get(0).isRead());
        assertEquals("Unread", result.get(0).getTextContent());
    }

    @Test
    void findByUserIdAndIsReadFalse_noUnreadNotifications_returnsEmptyList() {
        // Arrange
        Notification read = new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Read", "Test Event"
        );
        read.setRead(true);
        read.setReadAt(LocalDateTime.now());
        notificationRepository.save(read);

        // Act
        List<Notification> result = notificationRepository.findByUserIdAndIsReadFalse(testUser1.getId());

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void save_notification_persistsAllFields() {
        // Arrange
        String textContent = "You are confirmed for the event: Test Event. Thank you for registering!";
        Notification notification = new Notification(
                testUser1,
                testEvent,
                NotificationType.REGISTRATION_CONFIRMATION,
                "en",
                textContent,
                "Test Event"
        );

        // Act
        Notification saved = notificationRepository.save(notification);
        Notification retrieved = notificationRepository.findById(saved.getId()).orElseThrow();

        // Assert
        assertNotNull(retrieved.getId());
        assertEquals(testUser1.getId(), retrieved.getUser().getId());
        assertEquals(testEvent.getId(), retrieved.getEvent().getId());
        assertEquals(NotificationType.REGISTRATION_CONFIRMATION, retrieved.getNotificationType());
        assertEquals("en", retrieved.getLanguage());
        assertEquals(textContent, retrieved.getTextContent());
        assertEquals("Test Event", retrieved.getEventTitle());
        assertFalse(retrieved.isRead());
        assertNull(retrieved.getReadAt());
        assertNotNull(retrieved.getCreatedAt());
    }

    @Test
    void update_notificationReadStatus_persistsChanges() {
        // Arrange
        Notification notification = new Notification(
                testUser1, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Text", "Test Event"
        );
        notification = notificationRepository.save(notification);
        Long notificationId = notification.getId();

        // Act
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        Notification updated = notificationRepository.findById(notificationId).orElseThrow();

        // Assert
        assertTrue(updated.isRead());
        assertNotNull(updated.getReadAt());
    }
}

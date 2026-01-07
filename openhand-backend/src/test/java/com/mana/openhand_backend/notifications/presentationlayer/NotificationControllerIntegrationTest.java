package com.mana.openhand_backend.notifications.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    private User testUser;
    private Event testEvent;
    private static final String TEST_USER_EMAIL = "notification_test@example.com";

    @BeforeEach
    @Transactional
    void setUp() {
        notificationRepository.deleteAll();
        
        testUser = userRepository.findByEmail(TEST_USER_EMAIL).orElseGet(() -> {
            User newUser = new User(TEST_USER_EMAIL, "password123", Set.of("ROLE_MEMBER"));
            newUser.setPreferredLanguage("en");
            return userRepository.save(newUser);
        });

        testEvent = new Event(
                "Integration Test Event",
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
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    @Transactional
    void getNotifications_authenticatedUser_returnsNotificationsList() throws Exception {
        // Arrange
        Notification notification1 = new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "You are confirmed for the event", "Integration Test Event"
        );
        notification1 = notificationRepository.save(notification1);
        notificationRepository.flush();

        Notification notification2 = new Notification(
                testUser, testEvent, NotificationType.REMINDER,
                "en", "Reminder: Event starts soon", "Integration Test Event"
        );
        notification2 = notificationRepository.save(notification2);
        notificationRepository.flush();

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(not(isEmptyOrNullString())));

        // Assert
        long notificationCount = notificationRepository.count();
        assertEquals(2, notificationCount);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void getNotifications_noNotifications_returnsEmptyList() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getNotifications_unauthenticatedUser_returnsUnauthorized() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void getUnreadCount_withUnreadNotifications_returnsCorrectCount() throws Exception {
        // Arrange
        notificationRepository.save(new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Unread 1", "Integration Test Event"
        ));
        notificationRepository.save(new Notification(
                testUser, testEvent, NotificationType.REMINDER,
                "en", "Unread 2", "Integration Test Event"
        ));

        Notification readNotification = new Notification(
                testUser, testEvent, NotificationType.CANCELLATION,
                "en", "Read", "Integration Test Event"
        );
        readNotification.setRead(true);
        readNotification.setReadAt(LocalDateTime.now());
        notificationRepository.save(readNotification);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void getUnreadCount_noUnreadNotifications_returnsZero() throws Exception {
        // Arrange
        Notification readNotification = new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Read", "Integration Test Event"
        );
        readNotification.setRead(true);
        readNotification.setReadAt(LocalDateTime.now());
        notificationRepository.save(readNotification);

        // Act & Assert
        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(0)));
    }

    @Test
    void getUnreadCount_unauthenticatedUser_returnsUnauthorized() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get("/api/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    @Transactional
    void markAsRead_unreadNotification_marksAsReadAndReturnsUpdatedNotification() throws Exception {
        // Arrange
        Notification notification = new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Unread notification", "Integration Test Event"
        );
        notification = notificationRepository.save(notification);
        Long notificationId = notification.getId();

        // Act & Assert
        mockMvc.perform(put("/api/notifications/{id}/read", notificationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert
        Notification updated = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AssertionError("Notification not found after markAsRead"));
        assertTrue(updated.isRead());
        assertFalse(updated.getReadAt() == null);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void markAsRead_nonExistentNotification_returnsInternalServerError() throws Exception {
        // Arrange
        Long nonExistentId = 99999L;

        // Act & Assert
        try {
            mockMvc.perform(put("/api/notifications/{id}/read", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // Expected exception from service layer
        }
    }

    @Test
    void markAsRead_unauthenticatedUser_returnsUnauthorized() throws Exception {
        // Arrange
        Notification notification = new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Unread", "Integration Test Event"
        );
        notification = notificationRepository.save(notification);

        // Act & Assert
        mockMvc.perform(put("/api/notifications/{id}/read", notification.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    @Transactional
    void markAllAsRead_multipleUnreadNotifications_marksAllAsRead() throws Exception {
        // Arrange
        notificationRepository.save(new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Unread 1", "Integration Test Event"
        ));
        notificationRepository.save(new Notification(
                testUser, testEvent, NotificationType.REMINDER,
                "en", "Unread 2", "Integration Test Event"
        ));
        notificationRepository.save(new Notification(
                testUser, testEvent, NotificationType.CANCELLATION,
                "en", "Unread 3", "Integration Test Event"
        ));

        // Act
        mockMvc.perform(put("/api/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(testUser.getId());
        assertEquals(0, unreadCount);
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void markAllAsRead_noUnreadNotifications_returnsOk() throws Exception {
        // Arrange
        Notification readNotification = new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Already read", "Integration Test Event"
        );
        readNotification.setRead(true);
        readNotification.setReadAt(LocalDateTime.now());
        notificationRepository.save(readNotification);

        // Act & Assert
        mockMvc.perform(put("/api/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void markAllAsRead_unauthenticatedUser_returnsUnauthorized() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(put("/api/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    @Transactional
    void deleteNotification_existingNotification_deletesSuccessfully() throws Exception {
        // Arrange
        Notification notification = new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "To be deleted", "Integration Test Event"
        );
        notification = notificationRepository.save(notification);
        Long notificationId = notification.getId();

        // Act
        mockMvc.perform(delete("/api/notifications/{id}", notificationId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Assert
        assertFalse(notificationRepository.existsById(notificationId));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"MEMBER"})
    void deleteNotification_nonExistentNotification_returnsInternalServerError() throws Exception {
        // Arrange
        Long nonExistentId = 99999L;

        // Act & Assert
        try {
            mockMvc.perform(delete("/api/notifications/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        } catch (Exception e) {
            // Expected exception from service layer
        }
    }

    @Test
    void deleteNotification_unauthenticatedUser_returnsUnauthorized() throws Exception {
        // Arrange
        Notification notification = new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "To be deleted", "Integration Test Event"
        );
        notification = notificationRepository.save(notification);

        // Act & Assert
        mockMvc.perform(delete("/api/notifications/{id}", notification.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"EMPLOYEE"})
    void getNotifications_employeeRole_hasAccess() throws Exception {
        // Arrange
        notificationRepository.save(new Notification(
                testUser, testEvent, NotificationType.REGISTRATION_CONFIRMATION,
                "en", "Employee notification", "Integration Test Event"
        ));

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, roles = {"GUEST"})
    void getNotifications_guestRole_accessDenied() throws Exception {
        // Arrange

        // Act & Assert
        mockMvc.perform(get("/api/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("Expected false but was true");
        }
    }

        private static void assertTrue(boolean condition) {
                if (!condition) {
                        throw new AssertionError("Expected true but was false");
                }
        }
}

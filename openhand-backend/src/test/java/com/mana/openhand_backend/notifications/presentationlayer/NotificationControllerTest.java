package com.mana.openhand_backend.notifications.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationResponseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController(notificationService, userRepository);
    }

    @Test
    void getNotifications_authenticatedUser_returnsNotificationsList() {
        // Arrange
        String userEmail = "test@example.com";
        Long userId = 1L;
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));

        Notification notification1 = createMockNotification(1L, "Event 1", NotificationType.REGISTRATION_CONFIRMATION, false);
        Notification notification2 = createMockNotification(2L, "Event 2", NotificationType.REMINDER, true);
        List<Notification> notifications = List.of(notification1, notification2);

        when(notificationService.getUserNotifications(userId)).thenReturn(notifications);

        // Act
        List<NotificationResponseModel> result = notificationController.getNotifications(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(notificationService).getUserNotifications(userId);
        verify(userRepository).findByEmail(userEmail);
    }

    @Test
    void getNotifications_userNotFoundInDb_throwsRuntimeException() {
        // Arrange
        String userEmail = "test@example.com";
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                notificationController.getNotifications(authentication)
        );
        assertTrue(exception.getMessage().contains("User not found"));
        verify(userRepository).findByEmail(userEmail);
        verify(notificationService, never()).getUserNotifications(any());
    }

    @Test
    void getUnreadCount_authenticatedUser_returnsCorrectCount() {
        // Arrange
        String userEmail = "test@example.com";
        Long userId = 1L;
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        // Act
        NotificationController.UnreadCountResponse result = notificationController.getUnreadCount(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.getCount());
        verify(notificationService).getUnreadCount(userId);
    }

    @Test
    void getUnreadCount_noUnreadNotifications_returnsZero() {
        // Arrange
        String userEmail = "test@example.com";
        Long userId = 1L;
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));
        when(notificationService.getUnreadCount(userId)).thenReturn(0L);

        // Act
        NotificationController.UnreadCountResponse result = notificationController.getUnreadCount(authentication);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getCount());
        verify(notificationService).getUnreadCount(userId);
    }

    @Test
    void markAsRead_validNotificationId_returnsUpdatedNotification() {
        // Arrange
        Long notificationId = 1L;
        Notification mockNotification = createMockNotification(notificationId, "Test Event", NotificationType.REGISTRATION_CONFIRMATION, true);
        when(notificationService.markAsRead(notificationId)).thenReturn(mockNotification);

        // Act
        NotificationResponseModel result = notificationController.markAsRead(notificationId);

        // Assert
        assertNotNull(result);
        assertEquals(notificationId, result.getId());
        verify(notificationService).markAsRead(notificationId);
    }

    @Test
    void markAsRead_notificationNotFound_throwsException() {
        // Arrange
        Long notificationId = 999L;
        when(notificationService.markAsRead(notificationId))
                .thenThrow(new RuntimeException("Notification not found"));

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                notificationController.markAsRead(notificationId)
        );
        verify(notificationService).markAsRead(notificationId);
    }

    @Test
    void markAllAsRead_authenticatedUser_marksAllNotificationsAsRead() {
        // Arrange
        String userEmail = "test@example.com";
        Long userId = 1L;
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(mockUser));

        // Act
        notificationController.markAllAsRead(authentication);

        // Assert
        verify(notificationService).markAllAsRead(userId);
        verify(userRepository).findByEmail(userEmail);
    }

    @Test
    void deleteNotification_validNotificationId_deletesSuccessfully() {
        // Arrange
        Long notificationId = 1L;
        doNothing().when(notificationService).deleteNotification(notificationId);

        // Act
        notificationController.deleteNotification(notificationId);

        // Assert
        verify(notificationService).deleteNotification(notificationId);
    }

    @Test
    void deleteNotification_notificationNotFound_throwsException() {
        // Arrange
        Long notificationId = 999L;
        doThrow(new RuntimeException("Notification not found"))
                .when(notificationService).deleteNotification(notificationId);

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                notificationController.deleteNotification(notificationId)
        );
        verify(notificationService).deleteNotification(notificationId);
    }

    private Notification createMockNotification(Long id, String eventTitle, NotificationType type, boolean isRead) {
        Notification notification = mock(Notification.class);
        when(notification.getId()).thenReturn(id);
        when(notification.getEventTitle()).thenReturn(eventTitle);
        when(notification.getNotificationType()).thenReturn(type);
        when(notification.isRead()).thenReturn(isRead);
        when(notification.getCreatedAt()).thenReturn(LocalDateTime.now().minusHours(1));
        if (isRead) {
            when(notification.getReadAt()).thenReturn(LocalDateTime.now());
        } else {
            when(notification.getReadAt()).thenReturn(null);
        }
        when(notification.getTextContent()).thenReturn("Test notification text");
        when(notification.getEvent()).thenReturn(mock(com.mana.openhand_backend.events.dataaccesslayer.Event.class));
        return notification;
    }
}

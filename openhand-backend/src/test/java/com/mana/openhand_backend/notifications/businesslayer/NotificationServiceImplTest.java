package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.utils.NotificationTextGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NotificationTextGenerator textGenerator;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userRepository,
                eventRepository,
                textGenerator
        );
    }

    @Test
    void createNotification_validInputs_createsAndSavesNotification() {
        // Arrange
        Long userId = 1L;
        Long eventId = 2L;
        String notificationType = "REGISTRATION_CONFIRMATION";
        String language = "en";

        User mockUser = mock(User.class);
        Event mockEvent = mock(Event.class);
        when(mockEvent.getTitle()).thenReturn("Test Event");
        when(mockEvent.getStartDateTime()).thenReturn(LocalDateTime.now().plusDays(1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));
        when(textGenerator.generateText(any(NotificationType.class), eq("Test Event"), eq(language), any()))
                .thenReturn("You are confirmed for the event: Test Event. Thank you for registering!");

        Notification mockNotification = mock(Notification.class);
        when(notificationRepository.save(any(Notification.class))).thenReturn(mockNotification);

        // Act
        Notification result = notificationService.createNotification(userId, eventId, notificationType, language);

        // Assert
        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(eventRepository).findById(eventId);
        verify(textGenerator).generateText(eq(NotificationType.REGISTRATION_CONFIRMATION), eq("Test Event"), eq(language), any());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_userNotFound_throwsUserNotFoundException() {
        // Arrange
        Long userId = 1L;
        Long eventId = 2L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                notificationService.createNotification(userId, eventId, "REGISTRATION_CONFIRMATION", "en")
        );
        verify(userRepository).findById(userId);
        verify(eventRepository, never()).findById(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_eventNotFound_throwsEventNotFoundException() {
        // Arrange
        Long userId = 1L;
        Long eventId = 2L;
        User mockUser = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EventNotFoundException.class, () ->
                notificationService.createNotification(userId, eventId, "REGISTRATION_CONFIRMATION", "en")
        );
        verify(userRepository).findById(userId);
        verify(eventRepository).findById(eventId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_invalidNotificationType_throwsIllegalArgumentException() {
        // Arrange
        Long userId = 1L;
        Long eventId = 2L;
        User mockUser = mock(User.class);
        Event mockEvent = mock(Event.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(mockEvent));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                notificationService.createNotification(userId, eventId, "INVALID_TYPE", "en")
        );
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getUserNotifications_validUserId_returnsNotificationsSortedByCreatedAtDesc() {
        // Arrange
        Long userId = 1L;
        List<Notification> mockNotifications = List.of(mock(Notification.class), mock(Notification.class));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(mockNotifications);

        // Act
        List<Notification> result = notificationService.getUserNotifications(userId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(mockNotifications, result);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getUserNotifications_noNotifications_returnsEmptyList() {
        // Arrange
        Long userId = 1L;
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of());

        // Act
        List<Notification> result = notificationService.getUserNotifications(userId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getUnreadCount_validUserId_returnsCorrectCount() {
        // Arrange
        Long userId = 1L;
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

        // Act
        long result = notificationService.getUnreadCount(userId);

        // Assert
        assertEquals(5L, result);
        verify(notificationRepository).countByUserIdAndIsReadFalse(userId);
    }

    @Test
    void getUnreadCount_noUnreadNotifications_returnsZero() {
        // Arrange
        Long userId = 1L;
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(0L);

        // Act
        long result = notificationService.getUnreadCount(userId);

        // Assert
        assertEquals(0L, result);
        verify(notificationRepository).countByUserIdAndIsReadFalse(userId);
    }

    @Test
    void markAsRead_validNotificationId_marksNotificationAsReadAndSetsReadAt() {
        // Arrange
        Long notificationId = 1L;
        Notification mockNotification = mock(Notification.class);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(mockNotification));
        when(notificationRepository.save(mockNotification)).thenReturn(mockNotification);

        // Act
        Notification result = notificationService.markAsRead(notificationId);

        // Assert
        assertNotNull(result);
        verify(notificationRepository).findById(notificationId);
        verify(mockNotification).setRead(true);
        verify(mockNotification).setReadAt(any(LocalDateTime.class));
        verify(notificationRepository).save(mockNotification);
    }

    @Test
    void markAsRead_notificationNotFound_throwsRuntimeException() {
        // Arrange
        Long notificationId = 1L;
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                notificationService.markAsRead(notificationId)
        );
        assertTrue(exception.getMessage().contains("Notification not found"));
        verify(notificationRepository).findById(notificationId);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsRead_multipleUnreadNotifications_marksAllAsReadWithSameTimestamp() {
        // Arrange
        Long userId = 1L;
        Notification notification1 = mock(Notification.class);
        Notification notification2 = mock(Notification.class);
        List<Notification> unreadNotifications = List.of(notification1, notification2);
        when(notificationRepository.findByUserIdAndIsReadFalse(userId)).thenReturn(unreadNotifications);

        // Act
        notificationService.markAllAsRead(userId);

        // Assert
        verify(notificationRepository).findByUserIdAndIsReadFalse(userId);
        verify(notification1).setRead(true);
        verify(notification1).setReadAt(any(LocalDateTime.class));
        verify(notification2).setRead(true);
        verify(notification2).setReadAt(any(LocalDateTime.class));
        verify(notificationRepository).saveAll(unreadNotifications);
    }

    @Test
    void markAllAsRead_noUnreadNotifications_doesNotSaveAnything() {
        // Arrange
        Long userId = 1L;
        when(notificationRepository.findByUserIdAndIsReadFalse(userId)).thenReturn(List.of());

        // Act
        notificationService.markAllAsRead(userId);

        // Assert
        verify(notificationRepository).findByUserIdAndIsReadFalse(userId);
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void deleteNotification_validNotificationId_deletesNotification() {
        // Arrange
        Long notificationId = 1L;
        when(notificationRepository.existsById(notificationId)).thenReturn(true);

        // Act
        notificationService.deleteNotification(notificationId);

        // Assert
        verify(notificationRepository).existsById(notificationId);
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    void deleteNotification_notificationNotFound_throwsRuntimeException() {
        // Arrange
        Long notificationId = 1L;
        when(notificationRepository.existsById(notificationId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                notificationService.deleteNotification(notificationId)
        );
        assertTrue(exception.getMessage().contains("Notification not found"));
        verify(notificationRepository).existsById(notificationId);
        verify(notificationRepository, never()).deleteById(any());
    }
}

package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.events.utils.EventTitleResolver;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.utils.NotificationTextGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final NotificationTextGenerator textGenerator;
    private final NotificationPreferenceService preferenceService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                  UserRepository userRepository,
                                  EventRepository eventRepository,
                                  NotificationTextGenerator textGenerator,
                                  NotificationPreferenceService preferenceService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.textGenerator = textGenerator;
        this.preferenceService = preferenceService;
    }

    @Override
    @Transactional
    public Notification createNotification(Long userId, Long eventId, String notificationType, String language) {
        return createNotification(userId, eventId, notificationType, language, null);
    }

    @Override
    @Transactional
    public Notification createNotification(Long userId, Long eventId, String notificationType, String language, String participantName) {
        // Verify user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Verify event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        // Convert string to NotificationType enum
        NotificationType type = NotificationType.valueOf(notificationType);

        if (!preferenceService.isNotificationEnabled(userId, type)) {
            return null;
        }

        // Resolve event title from translation key to display name for text content
        String resolvedEventTitle = EventTitleResolver.resolve(event.getTitle(), language);

        // Generate notification text based on type and language using resolved title
        String textContent = textGenerator.generateText(type, resolvedEventTitle, language, event.getStartDateTime(), participantName);

        // Create notification entity with ORIGINAL translation key (not resolved)
        // The frontend will translate the title dynamically based on user's current language
        Notification notification = new Notification(
                user,
                event,
                type,
                language,
                textContent,
                event.getTitle(),  // Pass original translation key, not resolved title
                participantName
        );

        // Save and return
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        LocalDateTime now = LocalDateTime.now();

        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(now);
        }

        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new RuntimeException("Notification not found with id: " + notificationId);
        }
        notificationRepository.deleteById(notificationId);
    }
}

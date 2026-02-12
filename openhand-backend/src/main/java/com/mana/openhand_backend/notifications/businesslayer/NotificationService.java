package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import java.util.List;

public interface NotificationService {

    /**
     * Create a notification for a user
     * 
     * @param userId the user ID
     * @param eventId the event ID
     * @param notificationType the type of notification
     * @param language the user's preferred language
     * @return the created notification
     */
    Notification createNotification(Long userId, Long eventId, String notificationType, String language);

    /**
     * Create a notification for an employee/admin who registered someone else
     * 
     * @param userId the user ID (employee/admin)
     * @param eventId the event ID
     * @param notificationType the type of notification
     * @param language the user's preferred language
     * @param participantName the name of the person who was registered
     * @return the created notification
     */
    Notification createNotification(Long userId, Long eventId, String notificationType, String language, String participantName);

    /**
     * Get all notifications for a user, ordered by newest first
     */
    List<Notification> getUserNotifications(Long userId);
    /**
     * Create a donation notification for a user
     * 
     * @param userId the user ID
     * @param language the user's preferred language
     * @return the created donation notification
     */
    Notification createDonationNotification(Long userId, String language);

    /**
     * Get count of unread notifications for a user
     */
    long getUnreadCount(Long userId);

    /**
     * Mark a notification as read
     */
    Notification markAsRead(Long notificationId);

    /**
     * Mark all notifications as read for a user
     */
    void markAllAsRead(Long userId);

    /**
     * Delete a notification
     */
    void deleteNotification(Long notificationId);
}

package com.mana.openhand_backend.notifications.dataaccesslayer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Find all notifications for a user, ordered by creation date (newest first)
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Count unread notifications for a user
     */
    long countByUserIdAndIsReadFalse(Long userId);
    
    /**
     * Find notifications by event and type (useful for checking if notification already exists)
     */
    List<Notification> findByEventIdAndNotificationType(Long eventId, NotificationType notificationType);
    
    /**
     * Find unread notifications for a user
     */
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
}

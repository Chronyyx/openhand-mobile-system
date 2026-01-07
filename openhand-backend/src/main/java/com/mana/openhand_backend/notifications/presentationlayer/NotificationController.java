package com.mana.openhand_backend.notifications.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.businesslayer.NotificationService;
import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationResponseModel;
import com.mana.openhand_backend.notifications.utils.NotificationResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    /**
     * Get all notifications for the current user
     */
    @GetMapping
    @PreAuthorize("hasRole('ROLE_MEMBER') or hasRole('ROLE_EMPLOYEE')")
    public List<NotificationResponseModel> getNotifications(Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return notifications.stream()
                .map(NotificationResponseMapper::toResponseModel)
                .collect(Collectors.toList());
    }

    /**
     * Get count of unread notifications for the current user
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('ROLE_MEMBER') or hasRole('ROLE_EMPLOYEE')")
    public UnreadCountResponse getUnreadCount(Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        long count = notificationService.getUnreadCount(userId);
        return new UnreadCountResponse(count);
    }

    /**
     * Mark a specific notification as read
     */
    @PutMapping("/{notificationId}/read")
    @PreAuthorize("hasRole('ROLE_MEMBER') or hasRole('ROLE_EMPLOYEE')")
    public NotificationResponseModel markAsRead(@PathVariable Long notificationId) {
        Notification notification = notificationService.markAsRead(notificationId);
        return NotificationResponseMapper.toResponseModel(notification);
    }

    /**
     * Mark all notifications as read for the current user
     */
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('ROLE_MEMBER') or hasRole('ROLE_EMPLOYEE')")
    @ResponseStatus(HttpStatus.OK)
    public void markAllAsRead(Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        notificationService.markAllAsRead(userId);
    }

    /**
     * Delete a specific notification
     */
    @DeleteMapping("/{notificationId}")
    @PreAuthorize("hasRole('ROLE_MEMBER') or hasRole('ROLE_EMPLOYEE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
    }

    /**
     * Helper method to extract user ID from authentication
     */
    private Long extractUserIdFromAuth(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userDetails.getUsername()));
        return user.getId();
    }

    /**
     * Response DTO for unread count endpoint
     */
    public static class UnreadCountResponse {
        private long count;

        public UnreadCountResponse(long count) {
            this.count = count;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}

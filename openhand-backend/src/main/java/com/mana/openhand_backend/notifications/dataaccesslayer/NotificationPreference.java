package com.mana.openhand_backend.notifications.dataaccesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import jakarta.persistence.*;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "confirmation_enabled", nullable = false)
    private boolean confirmationEnabled = true;

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled = true;

    @Column(name = "cancellation_enabled", nullable = false)
    private boolean cancellationEnabled = true;

    protected NotificationPreference() {
    }

    public NotificationPreference(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public boolean isConfirmationEnabled() {
        return confirmationEnabled;
    }

    public void setConfirmationEnabled(boolean confirmationEnabled) {
        this.confirmationEnabled = confirmationEnabled;
    }

    public boolean isReminderEnabled() {
        return reminderEnabled;
    }

    public void setReminderEnabled(boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public boolean isCancellationEnabled() {
        return cancellationEnabled;
    }

    public void setCancellationEnabled(boolean cancellationEnabled) {
        this.cancellationEnabled = cancellationEnabled;
    }
}

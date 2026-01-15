package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreference;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceCategory;

import java.util.EnumSet;
import java.util.Set;

public final class NotificationPreferencePolicy {

    private static final Set<NotificationPreferenceCategory> CRITICAL_CATEGORIES =
            EnumSet.of(NotificationPreferenceCategory.CANCELLATION);

    private NotificationPreferencePolicy() {
    }

    public static boolean isCritical(NotificationPreferenceCategory category) {
        return CRITICAL_CATEGORIES.contains(category);
    }

    public static NotificationPreferenceCategory toCategory(NotificationType type) {
        return switch (type) {
            case REGISTRATION_CONFIRMATION, EMPLOYEE_REGISTERED_PARTICIPANT -> NotificationPreferenceCategory.CONFIRMATION;
            case REMINDER -> NotificationPreferenceCategory.REMINDER;
            case CANCELLATION -> NotificationPreferenceCategory.CANCELLATION;
        };
    }

    public static boolean isEnabled(NotificationPreference preference, NotificationPreferenceCategory category) {
        return switch (category) {
            case CONFIRMATION -> preference.isConfirmationEnabled();
            case REMINDER -> preference.isReminderEnabled();
            case CANCELLATION -> preference.isCancellationEnabled();
        };
    }

    public static void applyEnabled(NotificationPreference preference, NotificationPreferenceCategory category, boolean enabled) {
        switch (category) {
            case CONFIRMATION -> preference.setConfirmationEnabled(enabled);
            case REMINDER -> preference.setReminderEnabled(enabled);
            case CANCELLATION -> preference.setCancellationEnabled(enabled);
        }
    }
}

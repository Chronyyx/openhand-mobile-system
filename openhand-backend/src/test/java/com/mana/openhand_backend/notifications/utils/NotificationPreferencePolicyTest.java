package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreference;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferencePolicyTest {

    @Test
    void isCritical_onlyCancellationIsCritical() {
        assertTrue(NotificationPreferencePolicy.isCritical(NotificationPreferenceCategory.CANCELLATION));
        assertFalse(NotificationPreferencePolicy.isCritical(NotificationPreferenceCategory.CONFIRMATION));
        assertFalse(NotificationPreferencePolicy.isCritical(NotificationPreferenceCategory.REMINDER));
    }

    @Test
    void toCategory_mapsTypes() {
        assertEquals(NotificationPreferenceCategory.CONFIRMATION,
                NotificationPreferencePolicy.toCategory(NotificationType.REGISTRATION_CONFIRMATION));
        assertEquals(NotificationPreferenceCategory.CONFIRMATION,
                NotificationPreferencePolicy.toCategory(NotificationType.EMPLOYEE_REGISTERED_PARTICIPANT));
        assertEquals(NotificationPreferenceCategory.REMINDER,
                NotificationPreferencePolicy.toCategory(NotificationType.REMINDER));
        assertEquals(NotificationPreferenceCategory.CANCELLATION,
                NotificationPreferencePolicy.toCategory(NotificationType.CANCELLATION));
        assertEquals(NotificationPreferenceCategory.CANCELLATION,
                NotificationPreferencePolicy.toCategory(NotificationType.EVENT_UPDATE));
    }

    @Test
    void isEnabled_readsPreferenceFlags() {
        NotificationPreference pref = new NotificationPreference(
                new com.mana.openhand_backend.identity.dataaccesslayer.User(
                        "user@example.com", "pwd", java.util.Set.of("ROLE_MEMBER"))
        );
        pref.setConfirmationEnabled(true);
        pref.setReminderEnabled(false);
        pref.setCancellationEnabled(true);

        assertTrue(NotificationPreferencePolicy.isEnabled(pref, NotificationPreferenceCategory.CONFIRMATION));
        assertFalse(NotificationPreferencePolicy.isEnabled(pref, NotificationPreferenceCategory.REMINDER));
        assertTrue(NotificationPreferencePolicy.isEnabled(pref, NotificationPreferenceCategory.CANCELLATION));
    }

    @Test
    void applyEnabled_updatesPreferenceFlags() {
        NotificationPreference pref = new NotificationPreference(
                new com.mana.openhand_backend.identity.dataaccesslayer.User(
                        "user@example.com", "pwd", java.util.Set.of("ROLE_MEMBER"))
        );

        NotificationPreferencePolicy.applyEnabled(pref, NotificationPreferenceCategory.CONFIRMATION, true);
        NotificationPreferencePolicy.applyEnabled(pref, NotificationPreferenceCategory.REMINDER, true);
        NotificationPreferencePolicy.applyEnabled(pref, NotificationPreferenceCategory.CANCELLATION, false);

        assertTrue(pref.isConfirmationEnabled());
        assertTrue(pref.isReminderEnabled());
        assertFalse(pref.isCancellationEnabled());
    }
}

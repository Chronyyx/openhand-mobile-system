package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreference;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferencePolicyTest {

    @Test
    void toCategory_mapsCancellationAndUpdateToCancellation() {
        assertEquals(NotificationPreferenceCategory.CANCELLATION,
                NotificationPreferencePolicy.toCategory(NotificationType.CANCELLATION));
        assertEquals(NotificationPreferenceCategory.CANCELLATION,
                NotificationPreferencePolicy.toCategory(NotificationType.EVENT_UPDATE));
    }

    @Test
    void applyEnabled_setsConfirmationFlag() {
        NotificationPreference preference = new NotificationPreference(new User());
        preference.setConfirmationEnabled(false);

        NotificationPreferencePolicy.applyEnabled(preference, NotificationPreferenceCategory.CONFIRMATION, true);

        assertTrue(preference.isConfirmationEnabled());
    }

    @Test
    void isCritical_identifiesCancellationAsCritical() {
        assertTrue(NotificationPreferencePolicy.isCritical(NotificationPreferenceCategory.CANCELLATION));
        assertFalse(NotificationPreferencePolicy.isCritical(NotificationPreferenceCategory.CONFIRMATION));
    }
}

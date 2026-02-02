package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreference;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceResponseModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NotificationPreferenceResponseMapperTest {

    @Test
    void toResponseModel_buildsAllCategories() {
        NotificationPreference preference = new NotificationPreference(null);
        preference.setConfirmationEnabled(false);
        preference.setReminderEnabled(true);
        preference.setCancellationEnabled(false);

        NotificationPreferenceResponseModel response = NotificationPreferenceResponseMapper.toResponseModel(1L, preference);

        assertEquals(1L, response.getMemberId());
        assertEquals(4, response.getPreferences().size());
        assertTrue(response.getPreferences().stream().anyMatch(item ->
                item.getCategory().equals("CAPACITY_ALERT") && item.isEnabled()));
    }

    @Test
    void defaultConstructor_isCallable() {
        assertNotNull(new NotificationPreferenceResponseMapper());
    }
}

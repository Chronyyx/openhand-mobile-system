package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceResponseModel;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceUpdateRequestModel;

public interface NotificationPreferenceService {

    NotificationPreferenceResponseModel getPreferencesForUser(Long userId);

    NotificationPreferenceResponseModel updatePreferences(Long userId, NotificationPreferenceUpdateRequestModel request);

    boolean isNotificationEnabled(Long userId, NotificationType notificationType);
}

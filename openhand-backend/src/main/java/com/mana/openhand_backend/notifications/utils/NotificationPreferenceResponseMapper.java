package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreference;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceCategory;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceItemResponseModel;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceResponseModel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationPreferenceResponseMapper {

    public static NotificationPreferenceResponseModel toResponseModel(Long memberId, NotificationPreference preference) {
        List<NotificationPreferenceItemResponseModel> items = Arrays.stream(NotificationPreferenceCategory.values())
                .map(category -> new NotificationPreferenceItemResponseModel(
                        category.name(),
                        NotificationPreferencePolicy.isEnabled(preference, category),
                        NotificationPreferencePolicy.isCritical(category)))
                .collect(Collectors.toList());

        return new NotificationPreferenceResponseModel(memberId, items);
    }
}

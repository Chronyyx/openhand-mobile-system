package com.mana.openhand_backend.notifications.domainclientlayer;

import java.util.List;

public class NotificationPreferenceUpdateRequestModel {

    private List<NotificationPreferenceItemRequestModel> preferences;

    public NotificationPreferenceUpdateRequestModel() {
    }

    public NotificationPreferenceUpdateRequestModel(List<NotificationPreferenceItemRequestModel> preferences) {
        this.preferences = preferences;
    }

    public List<NotificationPreferenceItemRequestModel> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<NotificationPreferenceItemRequestModel> preferences) {
        this.preferences = preferences;
    }
}

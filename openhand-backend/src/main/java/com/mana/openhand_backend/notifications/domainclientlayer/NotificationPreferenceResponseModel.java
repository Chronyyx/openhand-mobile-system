package com.mana.openhand_backend.notifications.domainclientlayer;

import java.util.List;

public class NotificationPreferenceResponseModel {

    private Long memberId;
    private List<NotificationPreferenceItemResponseModel> preferences;

    public NotificationPreferenceResponseModel() {
    }

    public NotificationPreferenceResponseModel(Long memberId, List<NotificationPreferenceItemResponseModel> preferences) {
        this.memberId = memberId;
        this.preferences = preferences;
    }

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public List<NotificationPreferenceItemResponseModel> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<NotificationPreferenceItemResponseModel> preferences) {
        this.preferences = preferences;
    }
}

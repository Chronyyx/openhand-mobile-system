package com.mana.openhand_backend.notifications.domainclientlayer;

public class NotificationPreferenceItemRequestModel {

    private String category;
    private boolean enabled;

    public NotificationPreferenceItemRequestModel() {
    }

    public NotificationPreferenceItemRequestModel(String category, boolean enabled) {
        this.category = category;
        this.enabled = enabled;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

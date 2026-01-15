package com.mana.openhand_backend.notifications.domainclientlayer;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationPreferenceItemResponseModel {

    private String category;
    private boolean enabled;
    @JsonProperty("isCritical")
    private boolean critical;

    public NotificationPreferenceItemResponseModel() {
    }

    public NotificationPreferenceItemResponseModel(String category, boolean enabled, boolean critical) {
        this.category = category;
        this.enabled = enabled;
        this.critical = critical;
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

    @JsonProperty("isCritical")
    public boolean isCritical() {
        return critical;
    }

    @JsonProperty("isCritical")
    public void setCritical(boolean critical) {
        this.critical = critical;
    }
}

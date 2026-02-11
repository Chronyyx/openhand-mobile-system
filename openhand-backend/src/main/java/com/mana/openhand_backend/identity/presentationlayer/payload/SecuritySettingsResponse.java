package com.mana.openhand_backend.identity.presentationlayer.payload;

public class SecuritySettingsResponse {
    private boolean biometricsEnabled;

    public SecuritySettingsResponse() {
    }

    public SecuritySettingsResponse(boolean biometricsEnabled) {
        this.biometricsEnabled = biometricsEnabled;
    }

    public boolean isBiometricsEnabled() {
        return biometricsEnabled;
    }

    public void setBiometricsEnabled(boolean biometricsEnabled) {
        this.biometricsEnabled = biometricsEnabled;
    }
}

package com.mana.openhand_backend.identity.presentationlayer.payload;

public class UpdateSecuritySettingsRequest {
    private boolean biometricsEnabled;

    public boolean isBiometricsEnabled() {
        return biometricsEnabled;
    }

    public void setBiometricsEnabled(boolean biometricsEnabled) {
        this.biometricsEnabled = biometricsEnabled;
    }
}

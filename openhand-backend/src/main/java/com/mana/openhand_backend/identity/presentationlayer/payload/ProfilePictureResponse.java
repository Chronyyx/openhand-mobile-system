package com.mana.openhand_backend.identity.presentationlayer.payload;

public class ProfilePictureResponse {
    private String url;

    public ProfilePictureResponse(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

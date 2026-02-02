package com.mana.openhand_backend.common.presentationlayer.payload;

public class ImageUrlResponse {
    private String url;

    public ImageUrlResponse(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

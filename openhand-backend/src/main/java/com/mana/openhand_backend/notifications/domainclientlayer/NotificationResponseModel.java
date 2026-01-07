package com.mana.openhand_backend.notifications.domainclientlayer;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationResponseModel {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String notificationType;
    private String textContent;
    @JsonProperty("isRead")
    private boolean isRead;
    private String createdAt;
    private String readAt;

    public NotificationResponseModel() {
    }

    public NotificationResponseModel(Long id, Long eventId, String eventTitle, 
                                     String notificationType, String textContent, 
                                     boolean isRead, String createdAt, String readAt) {
        this.id = id;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.notificationType = notificationType;
        this.textContent = textContent;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getReadAt() {
        return readAt;
    }

    public void setReadAt(String readAt) {
        this.readAt = readAt;
    }
}

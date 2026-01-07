package com.mana.openhand_backend.notifications.utils;

import com.mana.openhand_backend.notifications.dataaccesslayer.Notification;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationResponseModel;
import java.time.format.DateTimeFormatter;

public class NotificationResponseMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static NotificationResponseModel toResponseModel(Notification notification) {
        return new NotificationResponseModel(
                notification.getId(),
                notification.getEvent().getId(),
                notification.getEventTitle(),
                notification.getNotificationType().toString(),
                notification.getTextContent(),
                notification.isRead(),
                notification.getCreatedAt().format(FORMATTER),
                notification.getReadAt() != null ? notification.getReadAt().format(FORMATTER) : null
        );
    }
}

package com.collabflow.notification.dto;

import java.time.Instant;
import java.util.UUID;

import com.collabflow.notification.Notification;
import com.collabflow.notification.NotificationType;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        String link,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getLink(),
                notification.getReadAt() != null,
                notification.getCreatedAt());
    }
}

package com.meetingplanner.common.api.dto;

import com.meetingplanner.domains.notification.model.NotificationEntity;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String payload,
        OffsetDateTime createdAt,
        boolean read
) {

    public static NotificationResponse from(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getPayload(),
                notification.getCreatedAt(),
                notification.getReadAt() != null
        );
    }
}
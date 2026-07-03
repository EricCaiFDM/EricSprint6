package com.example.banking.api.notifications.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for notification feed item schema.")
public record NotificationFeedItemSchema(
        String notificationId,
        String title,
        String message,
        String occurredAt,
        String level) {
}

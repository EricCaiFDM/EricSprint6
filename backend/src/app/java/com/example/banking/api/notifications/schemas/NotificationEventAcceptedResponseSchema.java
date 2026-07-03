package com.example.banking.api.notifications.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for notification event accepted response schema payload.")
public record NotificationEventAcceptedResponseSchema(
        String notificationEventId,
        String status) {
}

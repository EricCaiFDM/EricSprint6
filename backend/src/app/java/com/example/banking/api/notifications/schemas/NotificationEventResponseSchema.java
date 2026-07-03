package com.example.banking.api.notifications.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for notification event response schema payload.")
public record NotificationEventResponseSchema(
        String notificationEventId,
        String eventType,
        String recipientScopeType,
        String recipientScopeId,
        String status,
        String finalOutcome,
        String reasonCode,
        String triggeredAtUtc,
        String completedAtUtc) {
}

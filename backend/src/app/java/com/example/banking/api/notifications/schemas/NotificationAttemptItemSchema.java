package com.example.banking.api.notifications.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for notification attempt item schema.")
public record NotificationAttemptItemSchema(
        String attemptId,
        String channel,
        int attemptNumber,
        String status,
        String queuedAtUtc,
        String startedAtUtc,
        String completedAtUtc,
        String reasonCode,
        String providerReferenceId) {
}

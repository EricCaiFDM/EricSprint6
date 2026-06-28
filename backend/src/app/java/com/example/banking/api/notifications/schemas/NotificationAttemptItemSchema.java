package com.example.banking.api.notifications.schemas;

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

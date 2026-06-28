package com.example.banking.api.notifications.schemas;

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

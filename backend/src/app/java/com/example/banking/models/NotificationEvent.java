package com.example.banking.models;

import java.time.Instant;

public record NotificationEvent(
        String notificationEventId,
        String eventType,
        NotificationRecipientScopeType recipientScopeType,
        String recipientScopeId,
        NotificationEventStatus status,
        Instant triggeredAtUtc,
        Instant completedAtUtc,
        NotificationDeliveryFinalStatus finalOutcome,
        String reasonCode) {
}

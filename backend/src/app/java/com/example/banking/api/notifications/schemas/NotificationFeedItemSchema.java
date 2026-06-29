package com.example.banking.api.notifications.schemas;

public record NotificationFeedItemSchema(
        String notificationId,
        String title,
        String message,
        String occurredAt,
        String level) {
}

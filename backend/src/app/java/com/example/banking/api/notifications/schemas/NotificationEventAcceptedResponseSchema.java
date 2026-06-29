package com.example.banking.api.notifications.schemas;

public record NotificationEventAcceptedResponseSchema(
        String notificationEventId,
        String status) {
}

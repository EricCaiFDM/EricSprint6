package com.example.banking.api.notifications.schemas;

public record NotificationPreferencesResponseSchema(
        boolean pushEnabled,
        boolean emailEnabled,
        boolean smsEnabled,
        boolean marketingEnabled) {
}

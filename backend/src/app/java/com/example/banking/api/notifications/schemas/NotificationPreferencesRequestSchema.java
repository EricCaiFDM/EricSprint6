package com.example.banking.api.notifications.schemas;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesRequestSchema(
        @NotNull(message = "pushEnabled is required")
        Boolean pushEnabled,

        @NotNull(message = "emailEnabled is required")
        Boolean emailEnabled,

        @NotNull(message = "smsEnabled is required")
        Boolean smsEnabled,

        @NotNull(message = "marketingEnabled is required")
        Boolean marketingEnabled) {
}

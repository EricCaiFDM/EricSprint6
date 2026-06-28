package com.example.banking.api.notifications.schemas;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ListNotificationAttemptsSchema(
        @NotBlank(message = "notificationEventId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "notificationEventId must be a UUID")
        String notificationEventId,

        @Min(value = 1, message = "page must be at least 1")
        int page,

        @Min(value = 1, message = "pageSize must be at least 1")
        @Max(value = 100, message = "pageSize must be less than or equal to 100")
        int pageSize) {
}

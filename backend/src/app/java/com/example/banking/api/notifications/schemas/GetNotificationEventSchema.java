package com.example.banking.api.notifications.schemas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for get notification event schema.")
public record GetNotificationEventSchema(
        @NotBlank(message = "notificationEventId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "notificationEventId must be a UUID")
        String notificationEventId) {
}

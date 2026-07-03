package com.example.banking.api.notifications.schemas;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for notification preferences request schema payload.")
public record NotificationPreferencesRequestSchema(
        @NotNull(message = "depositAlertsEnabled is required")
        Boolean depositAlertsEnabled,

        @NotNull(message = "withdrawalAlertsEnabled is required")
        Boolean withdrawalAlertsEnabled,

        @NotNull(message = "transferAlertsEnabled is required")
        Boolean transferAlertsEnabled,

        @NotNull(message = "statementAlertsEnabled is required")
        Boolean statementAlertsEnabled,

        @NotNull(message = "offersEnabled is required")
        Boolean offersEnabled) {
}

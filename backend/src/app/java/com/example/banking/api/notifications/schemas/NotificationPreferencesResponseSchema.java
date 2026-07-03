package com.example.banking.api.notifications.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for notification preferences response schema payload.")
public record NotificationPreferencesResponseSchema(
        boolean depositAlertsEnabled,
        boolean withdrawalAlertsEnabled,
        boolean transferAlertsEnabled,
        boolean statementAlertsEnabled,
        boolean offersEnabled) {
}

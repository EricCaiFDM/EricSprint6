package com.example.banking.api.notifications.schemas;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for trigger notification schema.")
public record TriggerNotificationSchema(
        @NotBlank(message = "eventType is required")
        String eventType,

        @NotBlank(message = "recipientScopeType is required")
        @Pattern(
                regexp = "^(CUSTOMER|ACCOUNT|ADMIN)$",
                message = "recipientScopeType must be CUSTOMER, ACCOUNT, or ADMIN")
        String recipientScopeType,

        @NotBlank(message = "recipientScopeId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "recipientScopeId must be a UUID")
        String recipientScopeId,

        @NotBlank(message = "templateCode is required")
        String templateCode,

        @NotNull(message = "templateContext is required")
        Map<String, Object> templateContext) {
}

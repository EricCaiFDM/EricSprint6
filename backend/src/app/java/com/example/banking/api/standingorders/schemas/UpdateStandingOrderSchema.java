package com.example.banking.api.standingorders.schemas;

import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for update standing order schema.")
public record UpdateStandingOrderSchema(
        String amount,

        @Pattern(regexp = "^(DAILY|WEEKLY|MONTHLY)$", message = "cadence must be DAILY, WEEKLY, or MONTHLY")
        String cadence,

        String effectiveFromUtc,

        String effectiveToUtc,

        String retryPolicyCode) {
}

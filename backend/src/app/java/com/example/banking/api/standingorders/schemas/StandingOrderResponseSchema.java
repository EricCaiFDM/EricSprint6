package com.example.banking.api.standingorders.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for standing order response schema payload.")
public record StandingOrderResponseSchema(
        String standingOrderId,
        String sourceAccountId,
        String destinationAccountId,
        String amount,
        String cadence,
        String lifecycleState,
        String nextExecutionAtUtc,
        String effectiveFromUtc,
        String effectiveToUtc) {
}

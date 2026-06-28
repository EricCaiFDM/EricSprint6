package com.example.banking.api.standingorders.schemas;

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

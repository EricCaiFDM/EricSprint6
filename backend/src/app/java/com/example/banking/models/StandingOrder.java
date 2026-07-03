package com.example.banking.models;

import java.time.Instant;

public record StandingOrder(
        String standingOrderId,
        String sourceAccountId,
        String destinationAccountId,
        String amount,
        String currencyCode,
        StandingOrderCadence cadence,
        StandingOrderLifecycleState lifecycleState,
        Instant nextExecutionAtUtc,
        Instant effectiveFromUtc,
        Instant effectiveToUtc,
        String retryPolicyCode) {

    public static StandingOrder fromEntity(StandingOrderEntity entity) {
        return new StandingOrder(
                entity.getStandingOrderId(),
                entity.getSourceAccountId(),
                entity.getDestinationAccountId(),
                entity.getAmount().toPlainString(),
                entity.getCurrencyCode(),
                entity.getCadence(),
                entity.getLifecycleState(),
                entity.getNextExecutionAtUtc(),
                entity.getEffectiveFromUtc(),
                entity.getEffectiveToUtc(),
                entity.getRetryPolicyCode());
    }
}

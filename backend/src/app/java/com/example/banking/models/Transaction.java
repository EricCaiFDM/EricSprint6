package com.example.banking.models;

import java.time.Instant;

public record Transaction(
        String transactionId,
        String accountId,
        TransactionType transactionType,
        String amount,
        String currencyCode,
        Instant postedAtUtc,
        String correlationId,
        String balanceAfter) {

    public static Transaction fromEntity(TransactionEntity entity) {
        return new Transaction(
                entity.getTransactionId(),
                entity.getAccountId(),
                entity.getTransactionType(),
                entity.getAmount().toPlainString(),
                entity.getCurrencyCode(),
                entity.getPostedAtUtc(),
                entity.getCorrelationId(),
                entity.getBalanceAfter().toPlainString());
    }
}

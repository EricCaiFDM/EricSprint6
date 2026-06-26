package com.example.banking.api.transactions.schemas;

public record TransactionHistoryItemSchema(
        String transactionId,
        String accountId,
        String transactionType,
        String amount,
        String currencyCode,
        String postedAtUtc,
        String balanceAfter,
        String correlationId) {
}

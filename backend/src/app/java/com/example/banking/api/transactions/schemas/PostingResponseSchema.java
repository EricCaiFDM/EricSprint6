package com.example.banking.api.transactions.schemas;

public record PostingResponseSchema(
        String transactionId,
        String transactionType,
        String postedAmount,
        String currencyCode,
        String balanceAfter,
        String postedAtUtc) {
}

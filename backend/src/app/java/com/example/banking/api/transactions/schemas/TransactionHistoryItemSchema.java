package com.example.banking.api.transactions.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for transaction history item schema.")
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

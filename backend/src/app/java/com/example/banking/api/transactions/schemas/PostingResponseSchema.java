package com.example.banking.api.transactions.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for posting response schema payload.")
public record PostingResponseSchema(
        String transactionId,
        String transactionType,
        String postedAmount,
        String currencyCode,
        String balanceAfter,
        String postedAtUtc) {
}

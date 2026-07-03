package com.example.banking.api.transactions.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for transfer response schema payload.")
public record TransferResponseSchema(
        String transferId,
        String debitTransactionId,
        String creditTransactionId,
        String postedAmount,
        String currencyCode,
        String sourceBalanceAfter,
        String destinationBalanceAfter,
        String postedAtUtc) {
}

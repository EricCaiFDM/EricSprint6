package com.example.banking.api.transactions.schemas;

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

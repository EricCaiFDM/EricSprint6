package com.example.banking.api.account.dto;

public record AccountResponse(
        String accountId,
        String accountNumber,
        Integer checkingNumber,
        String customerId,
        String accountType,
        String interestRate,
        String status,
        String currencyCode,
        String nickname,
        String balance,
        String availableBalance,
        String currentBalance,
        String openedAtUtc,
        String closedAtUtc) {
}

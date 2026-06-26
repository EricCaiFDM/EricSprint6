package com.example.banking.api.account.dto;

public record AccountResponse(
        String accountId,
        String accountNumber,
        String customerId,
        String accountType,
        String status,
        String currencyCode,
        String nickname,
        String openedAtUtc,
        String closedAtUtc) {
}

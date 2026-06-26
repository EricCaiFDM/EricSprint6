package com.example.banking.api.account.dto;

public record ListAccountsRequest(
        String customerId,
        int page,
        int pageSize,
        String accountType,
        String status) {
}

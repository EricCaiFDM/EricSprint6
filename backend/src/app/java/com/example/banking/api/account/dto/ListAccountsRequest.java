package com.example.banking.api.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for list accounts request payload.")
public record ListAccountsRequest(
        String customerId,
        int page,
        int pageSize,
        String accountType,
        String status) {
}

package com.example.banking.api.account.dto;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for account list response payload.")
public record AccountListResponse(
        List<AccountResponse> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

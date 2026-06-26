package com.example.banking.api.account.dto;

import java.util.List;

public record AccountListResponse(
        List<AccountResponse> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

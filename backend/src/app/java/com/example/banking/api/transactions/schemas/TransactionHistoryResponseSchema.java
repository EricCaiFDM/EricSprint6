package com.example.banking.api.transactions.schemas;

import java.util.List;

public record TransactionHistoryResponseSchema(
        List<TransactionHistoryItemSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

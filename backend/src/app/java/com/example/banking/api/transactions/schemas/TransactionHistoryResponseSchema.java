package com.example.banking.api.transactions.schemas;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for transaction history response schema payload.")
public record TransactionHistoryResponseSchema(
        List<TransactionHistoryItemSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

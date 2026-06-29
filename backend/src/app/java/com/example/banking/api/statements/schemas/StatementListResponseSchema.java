package com.example.banking.api.statements.schemas;

import java.util.List;

public record StatementListResponseSchema(
        List<StatementListItemSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

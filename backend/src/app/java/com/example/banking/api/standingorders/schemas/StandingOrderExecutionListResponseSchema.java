package com.example.banking.api.standingorders.schemas;

import java.util.List;

public record StandingOrderExecutionListResponseSchema(
        List<StandingOrderExecutionItemSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

package com.example.banking.api.standingorders.schemas;

import java.util.List;

public record StandingOrderListResponseSchema(
        List<StandingOrderResponseSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

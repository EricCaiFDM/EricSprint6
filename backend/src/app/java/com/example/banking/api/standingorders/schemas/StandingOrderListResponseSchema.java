package com.example.banking.api.standingorders.schemas;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for standing order list response schema payload.")
public record StandingOrderListResponseSchema(
        List<StandingOrderResponseSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

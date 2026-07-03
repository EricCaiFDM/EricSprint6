package com.example.banking.api.standingorders.schemas;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for standing order execution list response schema payload.")
public record StandingOrderExecutionListResponseSchema(
        List<StandingOrderExecutionItemSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

package com.example.banking.api.insights;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for spending insight response schema payload.")
public record SpendingInsightResponseSchema(
        String periodLabel,
        String periodStartUtc,
        String periodEndUtc,
        String scopeType,
        String scopeId,
        String totalSpend,
        String currency,
        String confidenceLabel,
        String confidenceLevel,
        String coverageRatio,
        String confidenceReason,
        String status,
        String methodology,
        List<SpendingInsightCategorySchema> categories) {
}

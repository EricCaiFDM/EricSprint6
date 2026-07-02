package com.example.banking.api.insights;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for spending insight category schema.")
public record SpendingInsightCategorySchema(
        String category,
        String categoryCode,
        String amount,
        double ratio,
        String trend) {
}

package com.example.banking.api.insights;

public record SpendingInsightCategorySchema(
        String category,
        String categoryCode,
        String amount,
        double ratio,
        String trend) {
}

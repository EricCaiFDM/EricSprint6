package com.example.banking.api.insights;

import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.banking.services.insights.SpendingInsightServiceResult;

@Component
public class SpendingInsightResponseMapper {
    public SpendingInsightResponseSchema toSchema(SpendingInsightServiceResult result) {
        List<SpendingInsightCategorySchema> categories = result.categories().stream()
                .map(this::toCategorySchema)
                .toList();

        return new SpendingInsightResponseSchema(
                result.periodLabel(),
                result.periodStartUtc().toString(),
                result.periodEndUtc().toString(),
                result.scopeType(),
                result.scopeId(),
                result.totalSpend().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                result.currency(),
                confidenceLabel(result.confidenceLevel()),
                result.confidenceLevel(),
                result.coverageRatio().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                result.confidenceReason(),
                result.status(),
                result.methodology(),
                categories);
    }

    private SpendingInsightCategorySchema toCategorySchema(SpendingInsightServiceResult.CategoryResult result) {
        return new SpendingInsightCategorySchema(
                result.categoryLabel(),
                result.categoryCode(),
                result.amount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                result.ratio(),
                result.trend());
    }

    private String confidenceLabel(String confidenceLevel) {
        return switch (confidenceLevel) {
            case "LOW" -> "Low confidence";
            case "MEDIUM" -> "Medium confidence";
            default -> "High confidence";
        };
    }
}

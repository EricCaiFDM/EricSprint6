package com.example.banking.services.insights;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SpendingInsightServiceResult(
        String scopeType,
        String scopeId,
        Instant periodStartUtc,
        Instant periodEndUtc,
        String periodLabel,
        BigDecimal totalSpend,
        String currency,
        String confidenceLevel,
        BigDecimal coverageRatio,
        String confidenceReason,
        String status,
        String methodology,
        List<CategoryResult> categories) {

    public record CategoryResult(
            String categoryCode,
            String categoryLabel,
            BigDecimal amount,
            double ratio,
            String trend) {
    }
}

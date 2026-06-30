package com.example.banking.services.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SpendingInsightTrendService {
    private static final BigDecimal FLAT_THRESHOLD_PERCENT = new BigDecimal("5.0");

    public TrendComputation compute(
            BigDecimal currentTotalSpend,
            BigDecimal previousTotalSpend,
            Map<String, BigDecimal> currentCategoryAmounts,
            Map<String, BigDecimal> previousCategoryAmounts) {
        Map<String, String> categoryTrends = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : currentCategoryAmounts.entrySet()) {
            BigDecimal previous = previousCategoryAmounts.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            categoryTrends.put(entry.getKey(), classifyTrend(entry.getValue(), previous));
        }

        String trendDirection = classifyTrend(currentTotalSpend, previousTotalSpend).toUpperCase();
        BigDecimal deltaPercent = deltaPercent(currentTotalSpend, previousTotalSpend);
        return new TrendComputation(trendDirection, deltaPercent, categoryTrends);
    }

    private String classifyTrend(BigDecimal current, BigDecimal previous) {
        BigDecimal normalizedCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal normalizedPrevious = previous == null ? BigDecimal.ZERO : previous;

        if (normalizedPrevious.signum() == 0) {
            if (normalizedCurrent.signum() == 0) {
                return "flat";
            }
            return "up";
        }

        BigDecimal deltaPercent = normalizedCurrent
                .subtract(normalizedPrevious)
                .multiply(new BigDecimal("100"))
                .divide(normalizedPrevious.abs(), 4, RoundingMode.HALF_UP);

        if (deltaPercent.abs().compareTo(FLAT_THRESHOLD_PERCENT) < 0) {
            return "flat";
        }

        return deltaPercent.signum() >= 0 ? "up" : "down";
    }

    private BigDecimal deltaPercent(BigDecimal current, BigDecimal previous) {
        BigDecimal normalizedCurrent = current == null ? BigDecimal.ZERO : current;
        BigDecimal normalizedPrevious = previous == null ? BigDecimal.ZERO : previous;

        if (normalizedPrevious.signum() == 0) {
            return null;
        }

        return normalizedCurrent
                .subtract(normalizedPrevious)
                .multiply(new BigDecimal("100"))
                .divide(normalizedPrevious.abs(), 3, RoundingMode.HALF_UP);
    }

    public record TrendComputation(
            String trendDirection,
            BigDecimal trendDeltaPercent,
            Map<String, String> categoryTrends) {
    }
}

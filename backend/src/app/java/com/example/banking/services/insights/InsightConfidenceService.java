package com.example.banking.services.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.example.banking.lib.config.InsightsModuleConfig;

@Service
public class InsightConfidenceService {
    private final InsightsModuleConfig insightsModuleConfig;

    public InsightConfidenceService(InsightsModuleConfig insightsModuleConfig) {
        this.insightsModuleConfig = insightsModuleConfig;
    }

    public ConfidenceEvaluation evaluate(int spendingTransactionCount, int missingCategoryCount) {
        int minimumThreshold = insightsModuleConfig.getMinimumTransactionCount();
        int mediumThreshold = Math.max(minimumThreshold, insightsModuleConfig.getMediumConfidenceTransactionCount());

        BigDecimal coverageRatio = BigDecimal.valueOf(spendingTransactionCount)
                .multiply(new BigDecimal("100"))
                .divide(BigDecimal.valueOf(minimumThreshold), 2, RoundingMode.HALF_UP);
        if (coverageRatio.compareTo(new BigDecimal("100")) > 0) {
            coverageRatio = new BigDecimal("100.00");
        }

        String confidenceLevel;
        if (spendingTransactionCount >= mediumThreshold) {
            confidenceLevel = "HIGH";
        } else if (spendingTransactionCount >= minimumThreshold) {
            confidenceLevel = "MEDIUM";
        } else {
            confidenceLevel = "LOW";
        }

        if (missingCategoryCount > 0 && "HIGH".equals(confidenceLevel)) {
            confidenceLevel = "MEDIUM";
        }

        boolean minimumThresholdSatisfied = spendingTransactionCount >= minimumThreshold;

        String notes;
        if (spendingTransactionCount == 0) {
            notes = "No posted spending transactions were found in this period.";
        } else if (!minimumThresholdSatisfied) {
            notes = "Insights are based on a limited sample size and may shift as more spending occurs.";
        } else if (missingCategoryCount > 0) {
            notes = "Some transactions could not be mapped to a taxonomy category and were excluded from category breakdowns.";
        } else {
            notes = "Insights are based on posted spending transactions and a stable category sample.";
        }

        return new ConfidenceEvaluation(
                confidenceLevel,
                coverageRatio,
                notes,
                minimumThresholdSatisfied,
                missingCategoryCount);
    }

    public record ConfidenceEvaluation(
            String confidenceLevel,
            BigDecimal coverageRatio,
            String notes,
            boolean minimumThresholdSatisfied,
            int missingCategoryCount) {
    }
}

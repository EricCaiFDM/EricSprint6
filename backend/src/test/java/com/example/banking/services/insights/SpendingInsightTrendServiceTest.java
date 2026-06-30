package com.example.banking.services.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SpendingInsightTrendServiceTest {

    private final SpendingInsightTrendService service = new SpendingInsightTrendService();

    @Test
    void computeHandlesZeroPreviousAndZeroOrPositiveCurrent() {
        Map<String, BigDecimal> currentCategories = new LinkedHashMap<>();
        currentCategories.put("ZERO_CASE", BigDecimal.ZERO);
        currentCategories.put("UP_CASE", new BigDecimal("3.00"));

        SpendingInsightTrendService.TrendComputation result = service.compute(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                currentCategories,
                Map.of());

        assertEquals("FLAT", result.trendDirection());
        assertNull(result.trendDeltaPercent());
        assertEquals("flat", result.categoryTrends().get("ZERO_CASE"));
        assertEquals("up", result.categoryTrends().get("UP_CASE"));
    }

    @Test
    void computeReturnsFlatWhenDeltaMagnitudeIsBelowThreshold() {
        SpendingInsightTrendService.TrendComputation result = service.compute(
                new BigDecimal("104.00"),
                new BigDecimal("100.00"),
                Map.of("CAT", new BigDecimal("96.00")),
                Map.of("CAT", new BigDecimal("100.00")));

        assertEquals("FLAT", result.trendDirection());
        assertEquals(new BigDecimal("4.000"), result.trendDeltaPercent());
        assertEquals("flat", result.categoryTrends().get("CAT"));
    }

    @Test
    void computeReturnsUpWhenDeltaMeetsOrExceedsThreshold() {
        SpendingInsightTrendService.TrendComputation result = service.compute(
                new BigDecimal("105.00"),
                new BigDecimal("100.00"),
                Map.of("CAT", new BigDecimal("110.00")),
                Map.of("CAT", new BigDecimal("100.00")));

        assertEquals("UP", result.trendDirection());
        assertEquals(new BigDecimal("5.000"), result.trendDeltaPercent());
        assertEquals("up", result.categoryTrends().get("CAT"));
    }

    @Test
    void computeReturnsDownForNegativeChange() {
        SpendingInsightTrendService.TrendComputation result = service.compute(
                new BigDecimal("90.00"),
                new BigDecimal("100.00"),
                Map.of("CAT", new BigDecimal("80.00")),
                Map.of("CAT", new BigDecimal("100.00")));

        assertEquals("DOWN", result.trendDirection());
        assertEquals(new BigDecimal("-10.000"), result.trendDeltaPercent());
        assertEquals("down", result.categoryTrends().get("CAT"));
    }

    @Test
    void computeNormalizesNullTotalsAndCategoryAmounts() {
        Map<String, BigDecimal> currentCategories = new LinkedHashMap<>();
        currentCategories.put("NULL_CURRENT", null);

        SpendingInsightTrendService.TrendComputation downResult = service.compute(
                null,
                new BigDecimal("50.00"),
                currentCategories,
                Map.of("NULL_CURRENT", new BigDecimal("10.00")));

        assertEquals("DOWN", downResult.trendDirection());
        assertEquals(new BigDecimal("-100.000"), downResult.trendDeltaPercent());
        assertEquals("down", downResult.categoryTrends().get("NULL_CURRENT"));

        SpendingInsightTrendService.TrendComputation upResult = service.compute(
                new BigDecimal("10.00"),
                null,
                Map.of("NEW", new BigDecimal("2.00")),
                Map.of());

        assertEquals("UP", upResult.trendDirection());
        assertNull(upResult.trendDeltaPercent());
        assertEquals("up", upResult.categoryTrends().get("NEW"));
    }
}
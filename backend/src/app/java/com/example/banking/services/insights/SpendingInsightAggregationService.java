package com.example.banking.services.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.banking.models.TransactionEntity;

@Service
public class SpendingInsightAggregationService {
    private final TaxonomyMappingRepository taxonomyMappingRepository;

    public SpendingInsightAggregationService(TaxonomyMappingRepository taxonomyMappingRepository) {
        this.taxonomyMappingRepository = taxonomyMappingRepository;
    }

    public AggregationResult aggregate(List<TransactionEntity> transactions, Set<String> categoryFilters) {
        if (transactions == null || transactions.isEmpty()) {
            return new AggregationResult(BigDecimal.ZERO, List.of(), 0);
        }

        Map<String, MutableCategoryAggregate> byCategoryCode = new LinkedHashMap<>();
        BigDecimal totalSpend = BigDecimal.ZERO;
        int unmappedCategoryCount = 0;

        for (TransactionEntity transaction : transactions) {
            TaxonomyMappingRepository.TaxonomyMapping mapping = taxonomyMappingRepository.resolve(transaction);
            if (mapping == null) {
                unmappedCategoryCount++;
                continue;
            }

            if (!categoryFilters.isEmpty() && !categoryFilters.contains(mapping.categoryCode())) {
                continue;
            }

            BigDecimal amount = normalizeAmount(transaction);
            totalSpend = totalSpend.add(amount);

            MutableCategoryAggregate aggregate = byCategoryCode.computeIfAbsent(
                    mapping.categoryCode(),
                    ignored -> new MutableCategoryAggregate(mapping.categoryCode(), mapping.categoryLabel()));
            aggregate.amount = aggregate.amount.add(amount);
            aggregate.transactionCount = aggregate.transactionCount + 1;
        }

        if (byCategoryCode.isEmpty()) {
            return new AggregationResult(totalSpend, List.of(), unmappedCategoryCount);
        }

        List<CategoryAggregate> categories = new ArrayList<>();
        for (MutableCategoryAggregate aggregate : byCategoryCode.values()) {
            double ratio = totalSpend.signum() <= 0
                    ? 0
                    : aggregate.amount
                            .divide(totalSpend, 6, RoundingMode.HALF_UP)
                            .doubleValue();
            categories.add(new CategoryAggregate(
                    aggregate.categoryCode,
                    aggregate.categoryLabel,
                    aggregate.amount,
                    aggregate.transactionCount,
                    ratio));
        }

        categories.sort(Comparator
                .comparing(CategoryAggregate::amount, Comparator.reverseOrder())
                .thenComparing(CategoryAggregate::categoryLabel));

        return new AggregationResult(totalSpend, categories, unmappedCategoryCount);
    }

    private BigDecimal normalizeAmount(TransactionEntity transaction) {
        if (transaction == null || transaction.getAmount() == null) {
            return BigDecimal.ZERO;
        }
        return transaction.getAmount().abs();
    }

    private static final class MutableCategoryAggregate {
        private final String categoryCode;
        private final String categoryLabel;
        private BigDecimal amount = BigDecimal.ZERO;
        private int transactionCount = 0;

        private MutableCategoryAggregate(String categoryCode, String categoryLabel) {
            this.categoryCode = categoryCode;
            this.categoryLabel = categoryLabel;
        }
    }

    public record CategoryAggregate(
            String categoryCode,
            String categoryLabel,
            BigDecimal amount,
            int transactionCount,
            double ratio) {
    }

    public record AggregationResult(
            BigDecimal totalSpend,
            List<CategoryAggregate> categories,
            int unmappedCategoryCount) {
        public Map<String, BigDecimal> amountByCategoryCode() {
            Map<String, BigDecimal> amounts = new LinkedHashMap<>();
            for (CategoryAggregate category : categories) {
                amounts.put(category.categoryCode(), category.amount());
            }
            return amounts;
        }
    }
}

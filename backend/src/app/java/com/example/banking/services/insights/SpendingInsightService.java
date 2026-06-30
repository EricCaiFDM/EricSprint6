package com.example.banking.services.insights;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.insights.SpendingInsightQuery;
import com.example.banking.lib.config.InsightsModuleConfig;
import com.example.banking.lib.errors.InsightErrors;
import com.example.banking.lib.security.InsightAccessGuard;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.insights.InsightCategorySummary;
import com.example.banking.models.insights.InsightConfidenceMetadata;
import com.example.banking.models.insights.InsightRetrievalEvent;
import com.example.banking.models.insights.SpendingInsight;
import com.example.banking.models.insights.SpendingInsightRequest;
import com.example.banking.services.TransactionRepository;
import com.example.banking.services.insights.repository.InsightCategorySummaryRepository;
import com.example.banking.services.insights.repository.InsightConfidenceMetadataRepository;
import com.example.banking.services.insights.repository.InsightRetrievalEventRepository;
import com.example.banking.services.insights.repository.SpendingInsightRepository;
import com.example.banking.services.insights.repository.SpendingInsightRequestRepository;

@Service
public class SpendingInsightService {
    private static final DateTimeFormatter PERIOD_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy");

    private final InsightAccessGuard insightAccessGuard;
    private final InsightDataVisibilityService insightDataVisibilityService;
    private final SpendingInsightAggregationService spendingInsightAggregationService;
    private final SpendingInsightTrendService spendingInsightTrendService;
    private final InsightConfidenceService insightConfidenceService;
    private final TaxonomyMappingRepository taxonomyMappingRepository;
    private final TransactionRepository transactionRepository;
    private final SpendingInsightRequestRepository spendingInsightRequestRepository;
    private final SpendingInsightRepository spendingInsightRepository;
    private final InsightCategorySummaryRepository insightCategorySummaryRepository;
    private final InsightConfidenceMetadataRepository insightConfidenceMetadataRepository;
    private final InsightRetrievalEventRepository insightRetrievalEventRepository;
    private final InsightsModuleConfig insightsModuleConfig;

    public SpendingInsightService(
            InsightAccessGuard insightAccessGuard,
            InsightDataVisibilityService insightDataVisibilityService,
            SpendingInsightAggregationService spendingInsightAggregationService,
            SpendingInsightTrendService spendingInsightTrendService,
            InsightConfidenceService insightConfidenceService,
            TaxonomyMappingRepository taxonomyMappingRepository,
            TransactionRepository transactionRepository,
            SpendingInsightRequestRepository spendingInsightRequestRepository,
            SpendingInsightRepository spendingInsightRepository,
            InsightCategorySummaryRepository insightCategorySummaryRepository,
            InsightConfidenceMetadataRepository insightConfidenceMetadataRepository,
            InsightRetrievalEventRepository insightRetrievalEventRepository,
            InsightsModuleConfig insightsModuleConfig) {
        this.insightAccessGuard = insightAccessGuard;
        this.insightDataVisibilityService = insightDataVisibilityService;
        this.spendingInsightAggregationService = spendingInsightAggregationService;
        this.spendingInsightTrendService = spendingInsightTrendService;
        this.insightConfidenceService = insightConfidenceService;
        this.taxonomyMappingRepository = taxonomyMappingRepository;
        this.transactionRepository = transactionRepository;
        this.spendingInsightRequestRepository = spendingInsightRequestRepository;
        this.spendingInsightRepository = spendingInsightRepository;
        this.insightCategorySummaryRepository = insightCategorySummaryRepository;
        this.insightConfidenceMetadataRepository = insightConfidenceMetadataRepository;
        this.insightRetrievalEventRepository = insightRetrievalEventRepository;
        this.insightsModuleConfig = insightsModuleConfig;
    }

    @Transactional
    public SpendingInsightServiceResult getInsights(SpendingInsightQuery query, String actorUserId, String role) {
        String normalizedActorUserId = normalizeActor(actorUserId);
        String normalizedRole = normalizeRole(role);

        InsightAccessGuard.InsightScope scope;
        try {
            scope = insightAccessGuard.resolveAndAuthorize(query, normalizedActorUserId, normalizedRole);
        } catch (ApiErrorException exception) {
            recordPreValidationFailure(query, normalizedActorUserId, normalizedRole, exception);
            throw exception;
        }

        SpendingInsightRequest requestEntity = buildRequestEntity(scope, normalizedActorUserId);
        requestEntity = spendingInsightRequestRepository.save(requestEntity);

        try {
            Set<String> categoryFilters = validateCategoryFilters(scope.categoryFilters());

            List<TransactionEntity> currentTransactions = loadTransactions(
                    scope.scopeType(),
                    scope.scopeId(),
                    scope.periodStartUtc(),
                    scope.periodEndUtc());
            List<TransactionEntity> currentVisibleTransactions = insightDataVisibilityService
                    .filterVisibleSpendingTransactions(currentTransactions);
            SpendingInsightAggregationService.AggregationResult currentAggregation = spendingInsightAggregationService
                    .aggregate(currentVisibleTransactions, categoryFilters);

            Duration periodDuration = Duration.between(scope.periodStartUtc(), scope.periodEndUtc());
            Instant previousPeriodEnd = scope.periodStartUtc();
            Instant previousPeriodStart = previousPeriodEnd.minus(periodDuration);

            List<TransactionEntity> previousTransactions = loadTransactions(
                    scope.scopeType(),
                    scope.scopeId(),
                    previousPeriodStart,
                    previousPeriodEnd);
            List<TransactionEntity> previousVisibleTransactions = insightDataVisibilityService
                    .filterVisibleSpendingTransactions(previousTransactions);
            SpendingInsightAggregationService.AggregationResult previousAggregation = spendingInsightAggregationService
                    .aggregate(previousVisibleTransactions, categoryFilters);

            SpendingInsightTrendService.TrendComputation trend = spendingInsightTrendService.compute(
                    currentAggregation.totalSpend(),
                    previousAggregation.totalSpend(),
                    currentAggregation.amountByCategoryCode(),
                    previousAggregation.amountByCategoryCode());

            InsightConfidenceService.ConfidenceEvaluation confidence = insightConfidenceService.evaluate(
                    currentVisibleTransactions.size(),
                    currentAggregation.unmappedCategoryCount());

            String status = resolveStatus(currentVisibleTransactions.size(), confidence.confidenceLevel());
            String currencyCode = resolveCurrency(currentTransactions);

            SpendingInsight insightEntity = buildInsightEntity(
                    requestEntity,
                    currentAggregation,
                    currencyCode,
                    trend,
                    status);
            insightEntity = spendingInsightRepository.save(insightEntity);

            persistCategorySummaries(insightEntity.getInsightId(), currentAggregation.categories());
            persistConfidenceMetadata(insightEntity.getInsightId(), confidence);
            recordRetrievalEvent(requestEntity.getRequestId(), insightEntity.getInsightId(), normalizedActorUserId,
                    normalizedRole, scope.scopeType(), scope.scopeId(), "ALLOWED", null);

            return buildResult(
                    scope,
                    currentAggregation,
                    trend,
                    confidence,
                    status,
                    currencyCode);
        } catch (ApiErrorException exception) {
            recordRetrievalEvent(
                    requestEntity.getRequestId(),
                    null,
                    normalizedActorUserId,
                    normalizedRole,
                    scope.scopeType(),
                    scope.scopeId(),
                    mapFailureOutcome(exception),
                    exception.getCode());
            throw exception;
        } catch (RuntimeException exception) {
            recordRetrievalEvent(
                    requestEntity.getRequestId(),
                    null,
                    normalizedActorUserId,
                    normalizedRole,
                    scope.scopeType(),
                    scope.scopeId(),
                    "FAILED_DEPENDENCY",
                    "INSIGHT_DEPENDENCY_FAILURE");
            throw InsightErrors.dependencyFailure("Spending insights are temporarily unavailable");
        }
    }

    private SpendingInsightRequest buildRequestEntity(InsightAccessGuard.InsightScope scope, String actorUserId) {
        SpendingInsightRequest request = new SpendingInsightRequest();
        request.setScopeType(scope.scopeType());
        request.setScopeId(scope.scopeId());
        request.setPeriodStartUtc(scope.periodStartUtc());
        request.setPeriodEndUtc(scope.periodEndUtc());
        request.setCategoryFilters(String.join(",", scope.categoryFilters()));
        request.setRequestedByUserId(actorUserId);
        request.setRequestedAtUtc(Instant.now());
        return request;
    }

    private Set<String> validateCategoryFilters(List<String> categoryFilters) {
        if (categoryFilters == null || categoryFilters.isEmpty()) {
            return Set.of();
        }

        Set<String> supportedCategories = taxonomyMappingRepository.supportedCategoryCodes();
        Set<String> normalizedFilters = new LinkedHashSet<>();

        for (String filter : categoryFilters) {
            if (!supportedCategories.contains(filter)) {
                throw InsightErrors.validation(
                        "Unsupported category filter: " + filter,
                        "categoryFilters");
            }
            normalizedFilters.add(filter);
        }

        return normalizedFilters;
    }

    private List<TransactionEntity> loadTransactions(
            String scopeType,
            String scopeId,
            Instant periodStartUtc,
            Instant periodEndUtc) {
        if ("ACCOUNT".equals(scopeType)) {
            return transactionRepository.findAccountTransactionsForPeriod(scopeId, periodStartUtc, periodEndUtc);
        }
        return transactionRepository.findCustomerTransactionsForPeriod(scopeId, periodStartUtc, periodEndUtc);
    }

    private SpendingInsight buildInsightEntity(
            SpendingInsightRequest requestEntity,
            SpendingInsightAggregationService.AggregationResult aggregation,
            String currencyCode,
            SpendingInsightTrendService.TrendComputation trend,
            String status) {
        SpendingInsight insight = new SpendingInsight();
        insight.setRequestId(requestEntity.getRequestId());
        insight.setTaxonomyVersion(insightsModuleConfig.getTaxonomyVersion());
        insight.setGeneratedAtUtc(Instant.now());
        insight.setStatus(status);
        insight.setTotalSpendAmount(aggregation.totalSpend());
        insight.setCurrencyCode(currencyCode);
        insight.setTrendDirection(resolveTrendDirection(status, trend.trendDirection()));
        insight.setTrendDeltaPercent(trend.trendDeltaPercent());
        return insight;
    }

    private void persistCategorySummaries(
            String insightId,
            List<SpendingInsightAggregationService.CategoryAggregate> categories) {
        if (categories.isEmpty()) {
            return;
        }

        List<InsightCategorySummary> entities = new ArrayList<>();
        for (SpendingInsightAggregationService.CategoryAggregate category : categories) {
            InsightCategorySummary summary = new InsightCategorySummary();
            summary.setInsightId(insightId);
            summary.setCategoryCode(category.categoryCode());
            summary.setCategoryLabel(category.categoryLabel());
            summary.setAmount(category.amount());
            summary.setTransactionCount(category.transactionCount());
            summary.setPeriodSharePercent(BigDecimal.valueOf(category.ratio() * 100));
            entities.add(summary);
        }

        insightCategorySummaryRepository.saveAll(entities);
    }

    private void persistConfidenceMetadata(String insightId, InsightConfidenceService.ConfidenceEvaluation confidence) {
        InsightConfidenceMetadata metadata = new InsightConfidenceMetadata();
        metadata.setInsightId(insightId);
        metadata.setCoverageRatio(confidence.coverageRatio());
        metadata.setConfidenceLevel(confidence.confidenceLevel());
        metadata.setMissingCategoryCount(confidence.missingCategoryCount());
        metadata.setMinimumThresholdSatisfied(confidence.minimumThresholdSatisfied());
        metadata.setNotes(confidence.notes());
        insightConfidenceMetadataRepository.save(metadata);
    }

    private SpendingInsightServiceResult buildResult(
            InsightAccessGuard.InsightScope scope,
            SpendingInsightAggregationService.AggregationResult aggregation,
            SpendingInsightTrendService.TrendComputation trend,
            InsightConfidenceService.ConfidenceEvaluation confidence,
            String status,
            String currencyCode) {
        int maxCategories = insightsModuleConfig.getMaxCategories();

        List<SpendingInsightServiceResult.CategoryResult> categories = aggregation.categories().stream()
                .limit(maxCategories)
                .map(category -> new SpendingInsightServiceResult.CategoryResult(
                        category.categoryCode(),
                        category.categoryLabel(),
                        category.amount(),
                        category.ratio(),
                        trend.categoryTrends().getOrDefault(category.categoryCode(), "flat")))
                .toList();

        return new SpendingInsightServiceResult(
                scope.scopeType(),
                scope.scopeId(),
                scope.periodStartUtc(),
                scope.periodEndUtc(),
                formatPeriodLabel(scope.periodStartUtc()),
                aggregation.totalSpend(),
                currencyCode,
                confidence.confidenceLevel(),
                confidence.coverageRatio(),
                confidence.notes(),
                status,
                "Spending insights use posted debit transactions (withdrawals and transfer debits), grouped by approved taxonomy mappings and compared with the previous equivalent period.",
                categories);
    }

    private void recordRetrievalEvent(
            String requestId,
            String insightId,
            String actorUserId,
            String role,
            String scopeType,
            String scopeId,
            String outcome,
            String reasonCode) {
        InsightRetrievalEvent event = new InsightRetrievalEvent();
        event.setRequestId(requestId);
        event.setInsightId(insightId);
        event.setRequesterUserId(actorUserId);
        event.setRequesterRole(role);
        event.setScopeType(scopeType);
        event.setScopeId(scopeId);
        event.setOccurredAtUtc(Instant.now());
        event.setOutcome(outcome);
        event.setReasonCode(reasonCode);
        insightRetrievalEventRepository.save(event);
    }

    private void recordPreValidationFailure(
            SpendingInsightQuery query,
            String actorUserId,
            String role,
            ApiErrorException exception) {
        String scopeType = query.getScopeType() == null || query.getScopeType().isBlank()
                ? "UNKNOWN"
                : query.getScopeType().trim().toUpperCase(Locale.ROOT);
        String scopeId = query.getScopeId() == null || query.getScopeId().isBlank()
                ? "00000000-0000-0000-0000-000000000000"
                : query.getScopeId().trim();

        recordRetrievalEvent(
                java.util.UUID.randomUUID().toString(),
                null,
                actorUserId,
                role,
                scopeType,
                scopeId,
                mapFailureOutcome(exception),
                exception.getCode());
    }

    private String mapFailureOutcome(ApiErrorException exception) {
        if ("INSIGHT_FORBIDDEN".equals(exception.getCode())) {
            return "DENIED_PERMISSION";
        }
        if ("INSIGHT_VALIDATION_ERROR".equals(exception.getCode())) {
            return "INVALID_FILTER";
        }
        return "FAILED_DEPENDENCY";
    }

    private String resolveStatus(int spendingTransactionCount, String confidenceLevel) {
        if (spendingTransactionCount <= 0) {
            return "INSUFFICIENT_DATA";
        }
        if ("LOW".equals(confidenceLevel)) {
            return "PARTIAL";
        }
        return "GENERATED";
    }

    private String resolveTrendDirection(String status, String suggestedTrendDirection) {
        if ("INSUFFICIENT_DATA".equals(status)) {
            return "INSUFFICIENT_DATA";
        }
        return suggestedTrendDirection == null ? "FLAT" : suggestedTrendDirection;
    }

    private String resolveCurrency(List<TransactionEntity> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return "USD";
        }

        return transactions.stream()
                .map(TransactionEntity::getCurrencyCode)
                .filter(currency -> currency != null && !currency.isBlank())
                .findFirst()
                .orElse("USD");
    }

    private String formatPeriodLabel(Instant periodStartUtc) {
        return PERIOD_LABEL_FORMATTER.format(YearMonth.from(periodStartUtc.atZone(ZoneOffset.UTC)));
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "UNKNOWN";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}

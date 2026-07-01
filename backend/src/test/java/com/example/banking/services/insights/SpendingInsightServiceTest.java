package com.example.banking.services.insights;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.insights.SpendingInsightQuery;
import com.example.banking.lib.config.InsightsModuleConfig;
import com.example.banking.lib.errors.InsightErrors;
import com.example.banking.lib.security.InsightAccessGuard;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
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

class SpendingInsightServiceTest {

    private GuardDouble guardDouble;
    private VisibilityDouble visibilityDouble;
    private AggregationDouble aggregationDouble;
    private TrendDouble trendDouble;
    private ConfidenceDouble confidenceDouble;
    private TaxonomyDouble taxonomyDouble;
    private TransactionRepositoryDouble transactionRepositoryDouble;
    private SpendingInsightRequestRepositoryDouble requestRepositoryDouble;
    private SpendingInsightRepositoryDouble insightRepositoryDouble;
    private InsightCategorySummaryRepositoryDouble categorySummaryRepositoryDouble;
    private InsightConfidenceMetadataRepositoryDouble confidenceMetadataRepositoryDouble;
    private InsightRetrievalEventRepositoryDouble retrievalEventRepositoryDouble;
    private InsightsModuleConfig insightsModuleConfig;
    private SpendingInsightService service;

    @BeforeEach
    void setUp() {
        guardDouble = new GuardDouble();
        visibilityDouble = new VisibilityDouble();
        aggregationDouble = new AggregationDouble();
        trendDouble = new TrendDouble();
        confidenceDouble = new ConfidenceDouble();
        taxonomyDouble = new TaxonomyDouble();
        transactionRepositoryDouble = new TransactionRepositoryDouble();
        requestRepositoryDouble = new SpendingInsightRequestRepositoryDouble();
        insightRepositoryDouble = new SpendingInsightRepositoryDouble();
        categorySummaryRepositoryDouble = new InsightCategorySummaryRepositoryDouble();
        confidenceMetadataRepositoryDouble = new InsightConfidenceMetadataRepositoryDouble();
        retrievalEventRepositoryDouble = new InsightRetrievalEventRepositoryDouble();

        insightsModuleConfig = new InsightsModuleConfig();
        insightsModuleConfig.setTaxonomyVersion("v-test");
        insightsModuleConfig.setMaxCategories(2);

        service = new SpendingInsightService(
                guardDouble,
                visibilityDouble,
                aggregationDouble,
                trendDouble,
                confidenceDouble,
                taxonomyDouble,
                transactionRepositoryDouble.proxy(),
                requestRepositoryDouble.proxy(),
                insightRepositoryDouble.proxy(),
                categorySummaryRepositoryDouble.proxy(),
                confidenceMetadataRepositoryDouble.proxy(),
                retrievalEventRepositoryDouble.proxy(),
                insightsModuleConfig);
    }

    @Test
    void getInsightsRecordsPreValidationForbiddenFailureWithUnknownScopeDefaults() {
        guardDouble.exception = InsightErrors.forbidden();

        SpendingInsightQuery query = query(null, "   ", null);
        ApiErrorException exception = captureGetInsightsError(query, null, " ");

        assertNotNull(exception);
        assertEquals("INSIGHT_FORBIDDEN", exception.getCode());
        assertEquals(0, requestRepositoryDouble.saved.size());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());

        InsightRetrievalEvent event = retrievalEventRepositoryDouble.saved.get(0);
        assertEquals("anonymous", event.getRequesterUserId());
        assertEquals("UNKNOWN", event.getRequesterRole());
        assertEquals("UNKNOWN", event.getScopeType());
        assertEquals("00000000-0000-0000-0000-000000000000", event.getScopeId());
        assertEquals("DENIED_PERMISSION", event.getOutcome());
        assertEquals("INSIGHT_FORBIDDEN", event.getReasonCode());
    }

    @Test
    void getInsightsRecordsPreValidationValidationFailureWithNormalizedScopeValues() {
        guardDouble.exception = InsightErrors.validation("bad filter", "categoryFilters");

        SpendingInsightQuery query = query("customer", "  custom-scope-id  ", null);
        ApiErrorException exception = captureGetInsightsError(query, "actor-1", " customer ");

        assertNotNull(exception);
        assertEquals("INSIGHT_VALIDATION_ERROR", exception.getCode());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());

        InsightRetrievalEvent event = retrievalEventRepositoryDouble.saved.get(0);
        assertEquals("actor-1", event.getRequesterUserId());
        assertEquals("CUSTOMER", event.getRequesterRole());
        assertEquals("CUSTOMER", event.getScopeType());
        assertEquals("custom-scope-id", event.getScopeId());
        assertEquals("INVALID_FILTER", event.getOutcome());
        assertEquals("INSIGHT_VALIDATION_ERROR", event.getReasonCode());
    }

    @Test
    void getInsightsPreValidationHandlesBlankScopeTypeNullScopeIdAndBlankActorNullRole() {
        guardDouble.exception = InsightErrors.forbidden();

        SpendingInsightQuery query = query("   ", null, null);
        ApiErrorException exception = captureGetInsightsError(query, "   ", null);

        assertNotNull(exception);
        assertEquals("INSIGHT_FORBIDDEN", exception.getCode());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());

        InsightRetrievalEvent event = retrievalEventRepositoryDouble.saved.get(0);
        assertEquals("anonymous", event.getRequesterUserId());
        assertEquals("UNKNOWN", event.getRequesterRole());
        assertEquals("UNKNOWN", event.getScopeType());
        assertEquals("00000000-0000-0000-0000-000000000000", event.getScopeId());
    }

    @Test
    void getInsightsGeneratedFlowPersistsEntitiesLimitsResultCategoriesAndDefaultsMissingTrend() {
        Instant periodStart = Instant.parse("2026-06-01T00:00:00Z");
        Instant periodEnd = Instant.parse("2026-06-30T23:59:59Z");
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "ACCOUNT",
                "acc-1",
                periodStart,
                periodEnd,
                List.of("CASH_WITHDRAWAL", "CASH_WITHDRAWAL", "TRANSFER_OUT"));

        taxonomyDouble.supportedCodes = new LinkedHashSet<>(Set.of("CASH_WITHDRAWAL", "TRANSFER_OUT"));

        transactionRepositoryDouble.queueAccountResponse(List.of(
                transaction(TransactionType.WITHDRAWAL, " ", "40.00"),
                transaction(TransactionType.TRANSFER_DEBIT, "EUR", "60.00")));
        transactionRepositoryDouble.queueAccountResponse(List.of(
                transaction(TransactionType.WITHDRAWAL, "EUR", "10.00")));

        visibilityDouble.queueVisibleTransactions(List.of(transaction(TransactionType.WITHDRAWAL, "EUR", "40.00")));
        visibilityDouble.queueVisibleTransactions(List.of(transaction(TransactionType.WITHDRAWAL, "EUR", "10.00")));

        aggregationDouble.queueResult(new SpendingInsightAggregationService.AggregationResult(
                new BigDecimal("100.00"),
                List.of(
                        category("CASH_WITHDRAWAL", "Cash withdrawals", "50.00", 2, 0.50),
                        category("TRANSFER_OUT", "Transfers out", "30.00", 1, 0.30),
                        category("DEPOSIT_INCOME", "Deposits", "20.00", 1, 0.20)),
                0));
        aggregationDouble.queueResult(new SpendingInsightAggregationService.AggregationResult(
                new BigDecimal("10.00"),
                List.of(category("CASH_WITHDRAWAL", "Cash withdrawals", "10.00", 1, 1.0)),
                0));

        trendDouble.next = new SpendingInsightTrendService.TrendComputation(
                "UP",
                new BigDecimal("25.000"),
                Map.of("CASH_WITHDRAWAL", "up"));
        confidenceDouble.next = new InsightConfidenceService.ConfidenceEvaluation(
                "HIGH",
                new BigDecimal("100.00"),
                "Insights are stable",
                true,
                0);

        SpendingInsightServiceResult result = service.getInsights(
                query("ACCOUNT", "acc-1", null),
                "actor-1",
                " customer ");

        assertEquals("ACCOUNT", result.scopeType());
        assertEquals("acc-1", result.scopeId());
        assertEquals("GENERATED", result.status());
        assertEquals("EUR", result.currency());
        assertEquals(2, result.categories().size());
        assertEquals("up", result.categories().get(0).trend());
        assertEquals("flat", result.categories().get(1).trend());

        assertEquals(Set.of("CASH_WITHDRAWAL", "TRANSFER_OUT"), aggregationDouble.lastFilters);

        assertEquals(1, requestRepositoryDouble.saved.size());
        assertEquals(
                "CASH_WITHDRAWAL,CASH_WITHDRAWAL,TRANSFER_OUT",
                requestRepositoryDouble.saved.get(0).getCategoryFilters());
        assertEquals("actor-1", requestRepositoryDouble.saved.get(0).getRequestedByUserId());

        assertEquals(1, insightRepositoryDouble.saved.size());
        SpendingInsight insight = insightRepositoryDouble.saved.get(0);
        assertEquals("v-test", insight.getTaxonomyVersion());
        assertEquals("GENERATED", insight.getStatus());
        assertEquals("UP", insight.getTrendDirection());
        assertEquals(new BigDecimal("25.000"), insight.getTrendDeltaPercent());

        assertEquals(3, categorySummaryRepositoryDouble.saved.size());
        assertEquals(1, confidenceMetadataRepositoryDouble.saved.size());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());
        assertEquals("ALLOWED", retrievalEventRepositoryDouble.saved.get(0).getOutcome());
        assertNull(retrievalEventRepositoryDouble.saved.get(0).getReasonCode());
        assertEquals("CUSTOMER", retrievalEventRepositoryDouble.saved.get(0).getRequesterRole());
    }

    @Test
    void getInsightsPartialFlowUsesCustomerScopeAndFallsBackToUsdForBlankCurrencies() {
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "CUSTOMER",
                "cust-1",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                List.of());

        transactionRepositoryDouble.queueCustomerResponse(List.of(
                transaction(TransactionType.WITHDRAWAL, " ", "20.00"),
                transaction(TransactionType.WITHDRAWAL, null, "5.00")));
        transactionRepositoryDouble.queueCustomerResponse(List.of());

        visibilityDouble.queueVisibleTransactions(List.of(transaction(TransactionType.WITHDRAWAL, null, "20.00")));
        visibilityDouble.queueVisibleTransactions(List.of());

        aggregationDouble.queueResult(new SpendingInsightAggregationService.AggregationResult(
                new BigDecimal("20.00"),
                List.of(category("CASH_WITHDRAWAL", "Cash withdrawals", "20.00", 1, 1.0)),
                0));
        aggregationDouble.queueResult(new SpendingInsightAggregationService.AggregationResult(
                BigDecimal.ZERO,
                List.of(),
                0));

        trendDouble.next = new SpendingInsightTrendService.TrendComputation(
                null,
                new BigDecimal("1.000"),
                Map.of());
        confidenceDouble.next = new InsightConfidenceService.ConfidenceEvaluation(
                "LOW",
                new BigDecimal("50.00"),
                "Limited sample",
                false,
                0);

        SpendingInsightServiceResult result = service.getInsights(
                query("CUSTOMER", "cust-1", null),
                "actor-2",
                "customer");

        assertEquals("PARTIAL", result.status());
        assertEquals("USD", result.currency());
        assertEquals(2, transactionRepositoryDouble.customerPeriodCalls);
        assertEquals(0, transactionRepositoryDouble.accountPeriodCalls);
        assertEquals("FLAT", insightRepositoryDouble.saved.get(0).getTrendDirection());
        assertEquals("ALLOWED", retrievalEventRepositoryDouble.saved.get(0).getOutcome());
    }

    @Test
    void getInsightsInsufficientDataOverridesTrendAndSkipsCategorySummaryPersistence() {
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "ACCOUNT",
                "acc-2",
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-31T23:59:59Z"),
                List.of());

        transactionRepositoryDouble.queueAccountResponse(List.of());
        transactionRepositoryDouble.queueAccountResponse(List.of());

        visibilityDouble.queueVisibleTransactions(List.of());
        visibilityDouble.queueVisibleTransactions(List.of());

        aggregationDouble.queueResult(new SpendingInsightAggregationService.AggregationResult(
                BigDecimal.ZERO,
                List.of(),
                0));
        aggregationDouble.queueResult(new SpendingInsightAggregationService.AggregationResult(
                BigDecimal.ZERO,
                List.of(),
                0));

        trendDouble.next = new SpendingInsightTrendService.TrendComputation(
                "UP",
                null,
                Map.of());
        confidenceDouble.next = new InsightConfidenceService.ConfidenceEvaluation(
                "LOW",
                new BigDecimal("0.00"),
                "No data",
                false,
                0);

        SpendingInsightServiceResult result = service.getInsights(
                query("ACCOUNT", "acc-2", null),
                "actor-3",
                "ADMIN");

        assertEquals("INSUFFICIENT_DATA", result.status());
        assertEquals("USD", result.currency());
        assertTrue(result.categories().isEmpty());
        assertEquals("INSUFFICIENT_DATA", insightRepositoryDouble.saved.get(0).getTrendDirection());
        assertEquals(0, categorySummaryRepositoryDouble.saveAllCalls);
    }

    @Test
    void getInsightsUnsupportedCategoryFilterRecordsInvalidFilterOutcome() {
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "ACCOUNT",
                "acc-3",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                List.of("TRAVEL"));
        taxonomyDouble.supportedCodes = Set.of("CASH_WITHDRAWAL", "TRANSFER_OUT");

        ApiErrorException exception = captureGetInsightsError(
                query("ACCOUNT", "acc-3", "TRAVEL"),
                "actor-4",
                "ADMIN");

        assertNotNull(exception);
        assertEquals("INSIGHT_VALIDATION_ERROR", exception.getCode());
        assertEquals(1, requestRepositoryDouble.saved.size());
        assertEquals(0, insightRepositoryDouble.saved.size());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());
        assertEquals("INVALID_FILTER", retrievalEventRepositoryDouble.saved.get(0).getOutcome());
        assertEquals("INSIGHT_VALIDATION_ERROR", retrievalEventRepositoryDouble.saved.get(0).getReasonCode());
    }

    @Test
    void getInsightsApiExceptionInPipelineMapsFailureOutcomeToFailedDependency() {
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "ACCOUNT",
                "acc-4",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                List.of());

        transactionRepositoryDouble.queueAccountResponse(List.of(transaction(TransactionType.WITHDRAWAL, "USD", "10.00")));
        visibilityDouble.queueVisibleTransactions(List.of(transaction(TransactionType.WITHDRAWAL, "USD", "10.00")));

        aggregationDouble.apiException = new ApiErrorException(
                HttpStatus.CONFLICT,
                "SOMETHING_ELSE",
                "conflict",
                null);

        ApiErrorException exception = captureGetInsightsError(
                query("ACCOUNT", "acc-4", null),
                "actor-5",
                "ADMIN");

        assertNotNull(exception);
        assertEquals("SOMETHING_ELSE", exception.getCode());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());
        assertEquals("FAILED_DEPENDENCY", retrievalEventRepositoryDouble.saved.get(0).getOutcome());
        assertEquals("SOMETHING_ELSE", retrievalEventRepositoryDouble.saved.get(0).getReasonCode());
    }

    @Test
    void getInsightsApiExceptionInPipelineMapsForbiddenOutcomeToDeniedPermission() {
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "ACCOUNT",
                "acc-forbidden",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                List.of());

        transactionRepositoryDouble.queueAccountResponse(List.of(transaction(TransactionType.WITHDRAWAL, "USD", "10.00")));
        visibilityDouble.queueVisibleTransactions(List.of(transaction(TransactionType.WITHDRAWAL, "USD", "10.00")));
        aggregationDouble.apiException = InsightErrors.forbidden();

        ApiErrorException exception = captureGetInsightsError(
                query("ACCOUNT", "acc-forbidden", null),
                "actor-forbidden",
                "ADMIN");

        assertNotNull(exception);
        assertEquals("INSIGHT_FORBIDDEN", exception.getCode());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());
        assertEquals("DENIED_PERMISSION", retrievalEventRepositoryDouble.saved.get(0).getOutcome());
        assertEquals("INSIGHT_FORBIDDEN", retrievalEventRepositoryDouble.saved.get(0).getReasonCode());
        assertEquals(0, insightRepositoryDouble.saved.size());
    }

    @Test
    void getInsightsApiExceptionInPipelineMapsValidationOutcomeToInvalidFilter() {
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "ACCOUNT",
                "acc-validation",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                List.of());

        transactionRepositoryDouble.queueAccountResponse(List.of(transaction(TransactionType.WITHDRAWAL, "USD", "10.00")));
        visibilityDouble.queueVisibleTransactions(List.of(transaction(TransactionType.WITHDRAWAL, "USD", "10.00")));
        aggregationDouble.apiException = InsightErrors.validation("invalid filter", "categoryFilters");

        ApiErrorException exception = captureGetInsightsError(
                query("ACCOUNT", "acc-validation", null),
                "actor-validation",
                "ADMIN");

        assertNotNull(exception);
        assertEquals("INSIGHT_VALIDATION_ERROR", exception.getCode());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());
        assertEquals("INVALID_FILTER", retrievalEventRepositoryDouble.saved.get(0).getOutcome());
        assertEquals("INSIGHT_VALIDATION_ERROR", retrievalEventRepositoryDouble.saved.get(0).getReasonCode());
        assertEquals(0, insightRepositoryDouble.saved.size());
    }

    @Test
    void getInsightsRuntimeExceptionIsWrappedAsDependencyFailure() {
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "ACCOUNT",
                "acc-5",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                List.of());
        transactionRepositoryDouble.accountException = new RuntimeException("db unavailable");

        ApiErrorException exception = captureGetInsightsError(
                query("ACCOUNT", "acc-5", null),
                "actor-6",
                "ADMIN");

        assertNotNull(exception);
        assertEquals("INSIGHT_DEPENDENCY_FAILURE", exception.getCode());
        assertEquals(1, retrievalEventRepositoryDouble.saved.size());
        assertEquals("FAILED_DEPENDENCY", retrievalEventRepositoryDouble.saved.get(0).getOutcome());
        assertEquals("INSIGHT_DEPENDENCY_FAILURE", retrievalEventRepositoryDouble.saved.get(0).getReasonCode());
    }

    @Test
    void getInsightsRuntimeExceptionInCustomerScopeIsWrappedAsDependencyFailure() {
        guardDouble.scope = new InsightAccessGuard.InsightScope(
                "CUSTOMER",
                "cust-9",
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-30T23:59:59Z"),
                List.of());
        transactionRepositoryDouble.customerException = new RuntimeException("customer query failed");

        ApiErrorException exception = captureGetInsightsError(
                query("CUSTOMER", "cust-9", null),
                "actor-9",
                "CUSTOMER");

        assertNotNull(exception);
        assertEquals("INSIGHT_DEPENDENCY_FAILURE", exception.getCode());
        assertEquals(1, transactionRepositoryDouble.customerPeriodCalls);
        assertEquals(0, transactionRepositoryDouble.accountPeriodCalls);
        assertEquals("FAILED_DEPENDENCY", retrievalEventRepositoryDouble.saved.get(0).getOutcome());
        assertEquals("INSIGHT_DEPENDENCY_FAILURE", retrievalEventRepositoryDouble.saved.get(0).getReasonCode());
    }

    @Test
    void validateCategoryFiltersReturnsEmptySetForNullInput() {
        Set<String> normalized = invokeValidateCategoryFilters(null);
        assertTrue(normalized.isEmpty());
    }

    @Test
    void validateCategoryFiltersRejectsUnsupportedCategoryViaInvokeHelper() {
        taxonomyDouble.supportedCodes = Set.of("CASH_WITHDRAWAL");

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> invokeValidateCategoryFilters(List.of("TRAVEL")));

        assertEquals("INSIGHT_VALIDATION_ERROR", exception.getCode());
        assertEquals("categoryFilters", exception.getField());
    }

    @Test
    void validateCategoryFiltersReturnsDeduplicatedSetForSupportedValues() {
        taxonomyDouble.supportedCodes = Set.of("CASH_WITHDRAWAL", "TRANSFER_OUT");

        Set<String> normalized = invokeValidateCategoryFilters(List.of(
                "CASH_WITHDRAWAL",
                "TRANSFER_OUT",
                "CASH_WITHDRAWAL"));

        assertEquals(Set.of("CASH_WITHDRAWAL", "TRANSFER_OUT"), normalized);
    }

    @Test
    void resolveCurrencyReturnsUsdForNullTransactionsList() {
        assertEquals("USD", invokeResolveCurrency(null));
    }

    @Test
    void resolveCurrencyReturnsUsdForEmptyAndBlankCurrencyTransactions() {
        assertEquals("USD", invokeResolveCurrency(List.of()));

        List<TransactionEntity> blankCurrencies = List.of(
                transaction(TransactionType.WITHDRAWAL, " ", "1.00"),
                transaction(TransactionType.WITHDRAWAL, null, "2.00"));
        assertEquals("USD", invokeResolveCurrency(blankCurrencies));
    }

    @Test
    void resolveCurrencyReturnsFirstNonBlankCurrency() {
        List<TransactionEntity> mixedCurrencies = List.of(
                transaction(TransactionType.WITHDRAWAL, " ", "1.00"),
                transaction(TransactionType.WITHDRAWAL, "EUR", "2.00"),
                transaction(TransactionType.WITHDRAWAL, "USD", "3.00"));

        assertEquals("EUR", invokeResolveCurrency(mixedCurrencies));
    }

    @Test
    void invocationHandlersCoverSaveAndFallbackBranchesAcrossRepositoryDoubles() {
        TransactionRepository transactionRepository = transactionRepositoryDouble.proxy();
        TransactionEntity transaction = transaction(TransactionType.DEPOSIT, "USD", "1.00");
        assertSame(transaction, transactionRepository.save(transaction));
        List<TransactionEntity> batch = List.of(transaction);
        assertEquals(batch, transactionRepository.saveAll(batch));
        assertTrue(transactionRepository.findById("missing").isEmpty());
        assertTrue(transactionRepository.findAccountTransactionsForPeriod("acc", Instant.now(), Instant.now()).isEmpty());
        assertTrue(transactionRepository.findCustomerTransactionsForPeriod("cust", Instant.now(), Instant.now()).isEmpty());
        assertTrue(transactionRepository.findAccountHistory(
                "acc",
                Instant.now(),
                Instant.now(),
                null,
                PageRequest.of(0, 10)).isEmpty());
        assertTrue(transactionRepository.findCustomerHistory(
                "cust",
                Instant.now(),
                Instant.now(),
                null,
                PageRequest.of(0, 10)).isEmpty());

        SpendingInsightRequestRepository requestRepository = requestRepositoryDouble.proxy();
        SpendingInsightRequest request = new SpendingInsightRequest();
        assertNotNull(requestRepository.save(request).getRequestId());
        assertTrue(requestRepository.findById("missing").isEmpty());
        assertTrue(requestRepository.findAll().isEmpty());

        SpendingInsightRepository insightRepository = insightRepositoryDouble.proxy();
        SpendingInsight insight = new SpendingInsight();
        assertNotNull(insightRepository.save(insight).getInsightId());
        assertTrue(insightRepository.findById("missing").isEmpty());
        assertTrue(insightRepository.findAll().isEmpty());

        InsightCategorySummaryRepository categoryRepository = categorySummaryRepositoryDouble.proxy();
        InsightCategorySummary summary = new InsightCategorySummary();
        assertNotNull(categoryRepository.save(summary).getSummaryId());
        assertFalse(categoryRepository.saveAll(List.of(new InsightCategorySummary())).isEmpty());
        assertTrue(categoryRepository.findById("missing").isEmpty());
        assertTrue(categoryRepository.findAll().isEmpty());

        InsightConfidenceMetadataRepository confidenceRepository = confidenceMetadataRepositoryDouble.proxy();
        InsightConfidenceMetadata metadata = new InsightConfidenceMetadata();
        assertNotNull(confidenceRepository.save(metadata).getConfidenceId());
        assertTrue(confidenceRepository.findById("missing").isEmpty());
        assertTrue(confidenceRepository.findAll().isEmpty());

        InsightRetrievalEventRepository retrievalRepository = retrievalEventRepositoryDouble.proxy();
        InsightRetrievalEvent event = new InsightRetrievalEvent();
        assertNotNull(retrievalRepository.save(event).getEventId());
        assertNotNull(event.getOccurredAtUtc());
        assertTrue(retrievalRepository.findById("missing").isEmpty());
        assertTrue(retrievalRepository.findAll().isEmpty());
    }

    private ApiErrorException captureGetInsightsError(SpendingInsightQuery query, String actorUserId, String role) {
        ApiErrorException exception = null;
        try {
            service.getInsights(query, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private SpendingInsightQuery query(String scopeType, String scopeId, String categoryFilters) {
        SpendingInsightQuery query = new SpendingInsightQuery();
        query.setScopeType(scopeType);
        query.setScopeId(scopeId);
        query.setCategoryFilters(categoryFilters);
        return query;
    }

    private SpendingInsightAggregationService.CategoryAggregate category(
            String categoryCode,
            String categoryLabel,
            String amount,
            int transactionCount,
            double ratio) {
        return new SpendingInsightAggregationService.CategoryAggregate(
                categoryCode,
                categoryLabel,
                new BigDecimal(amount),
                transactionCount,
                ratio);
    }

    private TransactionEntity transaction(TransactionType type, String currencyCode, String amount) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setTransactionType(type);
        transaction.setCurrencyCode(currencyCode);
        transaction.setAmount(new BigDecimal(amount));
        return transaction;
    }

    @SuppressWarnings("unchecked")
    private Set<String> invokeValidateCategoryFilters(List<String> categoryFilters) {
        try {
            Method method = SpendingInsightService.class.getDeclaredMethod("validateCategoryFilters", List.class);
            method.setAccessible(true);
            return (Set<String>) method.invoke(service, categoryFilters);
        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            if (target instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private String invokeResolveCurrency(List<TransactionEntity> transactions) {
        try {
            Method method = SpendingInsightService.class.getDeclaredMethod("resolveCurrency", List.class);
            method.setAccessible(true);
            return (String) method.invoke(service, transactions);
        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            if (target instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static final class GuardDouble extends InsightAccessGuard {
        private InsightScope scope;
        private ApiErrorException exception;

        private GuardDouble() {
            super(null, null);
        }

        @Override
        public InsightScope resolveAndAuthorize(SpendingInsightQuery query, String actorUserId, String role) {
            if (exception != null) {
                throw exception;
            }
            return scope;
        }
    }

    private static final class VisibilityDouble extends InsightDataVisibilityService {
        private final List<List<TransactionEntity>> scriptedVisibleTransactions = new ArrayList<>();

        private void queueVisibleTransactions(List<TransactionEntity> transactions) {
            scriptedVisibleTransactions.add(transactions);
        }

        @Override
        public List<TransactionEntity> filterVisibleSpendingTransactions(List<TransactionEntity> transactions) {
            if (!scriptedVisibleTransactions.isEmpty()) {
                return scriptedVisibleTransactions.remove(0);
            }
            return super.filterVisibleSpendingTransactions(transactions);
        }
    }

    private static final class AggregationDouble extends SpendingInsightAggregationService {
        private final List<AggregationResult> scriptedResults = new ArrayList<>();
        private ApiErrorException apiException;
        private Set<String> lastFilters = Set.of();

        private AggregationDouble() {
            super(new TaxonomyMappingRepository());
        }

        private void queueResult(AggregationResult result) {
            scriptedResults.add(result);
        }

        @Override
        public AggregationResult aggregate(List<TransactionEntity> transactions, Set<String> categoryFilters) {
            lastFilters = new LinkedHashSet<>(categoryFilters);
            if (apiException != null) {
                throw apiException;
            }
            if (scriptedResults.isEmpty()) {
                return new AggregationResult(BigDecimal.ZERO, List.of(), 0);
            }
            return scriptedResults.remove(0);
        }
    }

    private static final class TrendDouble extends SpendingInsightTrendService {
        private TrendComputation next = new TrendComputation("UP", BigDecimal.ZERO, Map.of());

        @Override
        public TrendComputation compute(
                BigDecimal currentTotalSpend,
                BigDecimal previousTotalSpend,
                Map<String, BigDecimal> currentCategoryAmounts,
                Map<String, BigDecimal> previousCategoryAmounts) {
            return next;
        }
    }

    private static final class ConfidenceDouble extends InsightConfidenceService {
        private ConfidenceEvaluation next = new ConfidenceEvaluation(
                "HIGH",
                new BigDecimal("100.00"),
                "Stable",
                true,
                0);

        private ConfidenceDouble() {
            super(new InsightsModuleConfig());
        }

        @Override
        public ConfidenceEvaluation evaluate(int spendingTransactionCount, int missingCategoryCount) {
            return next;
        }
    }

    private static final class TaxonomyDouble extends TaxonomyMappingRepository {
        private Set<String> supportedCodes = Set.of("CASH_WITHDRAWAL", "TRANSFER_OUT", "DEPOSIT_INCOME");

        @Override
        public Set<String> supportedCategoryCodes() {
            return supportedCodes;
        }
    }

    private static final class TransactionRepositoryDouble implements InvocationHandler {
        private final List<List<TransactionEntity>> accountResponses = new ArrayList<>();
        private final List<List<TransactionEntity>> customerResponses = new ArrayList<>();
        private RuntimeException accountException;
        private RuntimeException customerException;
        private int accountPeriodCalls;
        private int customerPeriodCalls;

        private TransactionRepository proxy() {
            return (TransactionRepository) Proxy.newProxyInstance(
                    TransactionRepository.class.getClassLoader(),
                    new Class<?>[] { TransactionRepository.class },
                    this);
        }

        private void queueAccountResponse(List<TransactionEntity> transactions) {
            accountResponses.add(transactions);
        }

        private void queueCustomerResponse(List<TransactionEntity> transactions) {
            customerResponses.add(transactions);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();

            if ("findAccountTransactionsForPeriod".equals(methodName)) {
                accountPeriodCalls += 1;
                if (accountException != null) {
                    throw accountException;
                }
                if (!accountResponses.isEmpty()) {
                    return accountResponses.remove(0);
                }
                return List.of();
            }

            if ("findCustomerTransactionsForPeriod".equals(methodName)) {
                customerPeriodCalls += 1;
                if (customerException != null) {
                    throw customerException;
                }
                if (!customerResponses.isEmpty()) {
                    return customerResponses.remove(0);
                }
                return List.of();
            }

            if ("save".equals(methodName)) {
                return args[0];
            }

            if ("saveAll".equals(methodName)) {
                return args[0];
            }

            if (method.getReturnType().equals(Optional.class)) {
                return Optional.empty();
            }
            if (method.getReturnType().equals(List.class)) {
                return List.of();
            }
            if (method.getReturnType().equals(Page.class)) {
                return Page.empty();
            }
            return null;
        }
    }

    private static final class SpendingInsightRequestRepositoryDouble implements InvocationHandler {
        private final List<SpendingInsightRequest> saved = new ArrayList<>();

        private SpendingInsightRequestRepository proxy() {
            return (SpendingInsightRequestRepository) Proxy.newProxyInstance(
                    SpendingInsightRequestRepository.class.getClassLoader(),
                    new Class<?>[] { SpendingInsightRequestRepository.class },
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                SpendingInsightRequest request = (SpendingInsightRequest) args[0];
                if (request.getRequestId() == null) {
                    request.setRequestId(UUID.randomUUID().toString());
                }
                saved.add(request);
                return request;
            }

            if (method.getReturnType().equals(List.class)) {
                return List.of();
            }
            if (method.getReturnType().equals(Optional.class)) {
                return Optional.empty();
            }
            return null;
        }
    }

    private static final class SpendingInsightRepositoryDouble implements InvocationHandler {
        private final List<SpendingInsight> saved = new ArrayList<>();

        private SpendingInsightRepository proxy() {
            return (SpendingInsightRepository) Proxy.newProxyInstance(
                    SpendingInsightRepository.class.getClassLoader(),
                    new Class<?>[] { SpendingInsightRepository.class },
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                SpendingInsight insight = (SpendingInsight) args[0];
                if (insight.getInsightId() == null) {
                    insight.setInsightId(UUID.randomUUID().toString());
                }
                saved.add(insight);
                return insight;
            }

            if (method.getReturnType().equals(List.class)) {
                return List.of();
            }
            if (method.getReturnType().equals(Optional.class)) {
                return Optional.empty();
            }
            return null;
        }
    }

    private static final class InsightCategorySummaryRepositoryDouble implements InvocationHandler {
        private final List<InsightCategorySummary> saved = new ArrayList<>();
        private int saveAllCalls;

        private InsightCategorySummaryRepository proxy() {
            return (InsightCategorySummaryRepository) Proxy.newProxyInstance(
                    InsightCategorySummaryRepository.class.getClassLoader(),
                    new Class<?>[] { InsightCategorySummaryRepository.class },
                    this);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("saveAll".equals(method.getName())) {
                saveAllCalls += 1;
                Iterable<InsightCategorySummary> entities = (Iterable<InsightCategorySummary>) args[0];
                List<InsightCategorySummary> persisted = new ArrayList<>();
                for (InsightCategorySummary entity : entities) {
                    if (entity.getSummaryId() == null) {
                        entity.setSummaryId(UUID.randomUUID().toString());
                    }
                    saved.add(entity);
                    persisted.add(entity);
                }
                return persisted;
            }

            if ("save".equals(method.getName())) {
                InsightCategorySummary entity = (InsightCategorySummary) args[0];
                if (entity.getSummaryId() == null) {
                    entity.setSummaryId(UUID.randomUUID().toString());
                }
                saved.add(entity);
                return entity;
            }

            if (method.getReturnType().equals(List.class)) {
                return List.of();
            }
            if (method.getReturnType().equals(Optional.class)) {
                return Optional.empty();
            }
            return null;
        }
    }

    private static final class InsightConfidenceMetadataRepositoryDouble implements InvocationHandler {
        private final List<InsightConfidenceMetadata> saved = new ArrayList<>();

        private InsightConfidenceMetadataRepository proxy() {
            return (InsightConfidenceMetadataRepository) Proxy.newProxyInstance(
                    InsightConfidenceMetadataRepository.class.getClassLoader(),
                    new Class<?>[] { InsightConfidenceMetadataRepository.class },
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                InsightConfidenceMetadata metadata = (InsightConfidenceMetadata) args[0];
                if (metadata.getConfidenceId() == null) {
                    metadata.setConfidenceId(UUID.randomUUID().toString());
                }
                saved.add(metadata);
                return metadata;
            }

            if (method.getReturnType().equals(List.class)) {
                return List.of();
            }
            if (method.getReturnType().equals(Optional.class)) {
                return Optional.empty();
            }
            return null;
        }
    }

    private static final class InsightRetrievalEventRepositoryDouble implements InvocationHandler {
        private final List<InsightRetrievalEvent> saved = new ArrayList<>();

        private InsightRetrievalEventRepository proxy() {
            return (InsightRetrievalEventRepository) Proxy.newProxyInstance(
                    InsightRetrievalEventRepository.class.getClassLoader(),
                    new Class<?>[] { InsightRetrievalEventRepository.class },
                    this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("save".equals(method.getName())) {
                InsightRetrievalEvent event = (InsightRetrievalEvent) args[0];
                if (event.getEventId() == null) {
                    event.setEventId(UUID.randomUUID().toString());
                }
                if (event.getOccurredAtUtc() == null) {
                    event.setOccurredAtUtc(Instant.now());
                }
                saved.add(event);
                return event;
            }

            if (method.getReturnType().equals(List.class)) {
                return List.of();
            }
            if (method.getReturnType().equals(Optional.class)) {
                return Optional.empty();
            }
            return null;
        }
    }
}
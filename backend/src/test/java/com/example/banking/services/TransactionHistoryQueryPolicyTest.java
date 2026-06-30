package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.transactions.schemas.HistorySchema;
import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.models.TransactionType;
import com.example.banking.services.TransactionHistoryQueryPolicy.QueryPolicyResult;

class TransactionHistoryQueryPolicyTest {

    private final TransactionHistoryQueryPolicy policy = new TransactionHistoryQueryPolicy(config(25));

    @Test
    void normalizeReturnsExpectedDefaultsAndSortPageable() {
        HistorySchema request = baseRequest();

        QueryPolicyResult result = policy.normalize(request);
        assertNull(captureNormalizeError(request));

        assertEquals("ACCOUNT", result.scopeType());
        assertEquals(request.getScopeId(), result.scopeId());
        assertNull(result.startDateUtc());
        assertNull(result.endDateUtc());
        assertNull(result.transactionType());
        assertEquals(0, result.pageable().getPageNumber());
        assertEquals(20, result.pageable().getPageSize());
        assertEquals("postedAtUtc: DESC,transactionId: DESC", result.pageable().getSort().toString());
    }

    @Test
    void normalizeParsesDateRangeAndTransactionType() {
        HistorySchema request = baseRequest();
        request.setScopeType("customer");
        request.setStartDateUtc("2026-06-01T00:00:00Z");
        request.setEndDateUtc("2026-06-29T00:00:00Z");
        request.setTransactionType("transfer_debit");
        request.setPage(2);
        request.setPageSize(10);

        QueryPolicyResult result = policy.normalize(request);

        assertEquals("CUSTOMER", result.scopeType());
        assertEquals(Instant.parse("2026-06-01T00:00:00Z"), result.startDateUtc());
        assertEquals(Instant.parse("2026-06-29T00:00:00Z"), result.endDateUtc());
        assertEquals(TransactionType.TRANSFER_DEBIT, result.transactionType());
        assertEquals(1, result.pageable().getPageNumber());
        assertEquals(10, result.pageable().getPageSize());
    }

    @Test
    void normalizeValidatesScopeTypeAndScopeId() {
        HistorySchema request = baseRequest();
        request.setScopeType("branch");
        ApiErrorException scopeType = captureNormalizeError(request);
        assertEquals("scopeType", scopeType.getField());

        HistorySchema invalidScopeId = baseRequest();
        invalidScopeId.setScopeId("not-uuid");
        ApiErrorException scopeId = captureNormalizeError(invalidScopeId);
        assertEquals("scopeId", scopeId.getField());
    }

    @Test
    void normalizeValidatesDateAndTransactionType() {
        HistorySchema badDate = baseRequest();
        badDate.setStartDateUtc("invalid-date");
        ApiErrorException dateError = captureNormalizeError(badDate);
        assertEquals("startDateUtc", dateError.getField());

        HistorySchema badRange = baseRequest();
        badRange.setStartDateUtc("2026-06-29T00:00:00Z");
        badRange.setEndDateUtc("2026-06-01T00:00:00Z");
        ApiErrorException range = captureNormalizeError(badRange);
        assertEquals("startDateUtc", range.getField());

        HistorySchema badType = baseRequest();
        badType.setTransactionType("something");
        ApiErrorException type = captureNormalizeError(badType);
        assertEquals("transactionType", type.getField());
    }

    @Test
    void normalizeValidatesPageAndPageSizeBounds() {
        HistorySchema pageTooLow = baseRequest();
        pageTooLow.setPage(0);
        ApiErrorException pageError = captureNormalizeError(pageTooLow);
        assertEquals("page", pageError.getField());

        HistorySchema pageSizeTooLow = baseRequest();
        pageSizeTooLow.setPageSize(0);
        ApiErrorException pageSizeLow = captureNormalizeError(pageSizeTooLow);
        assertEquals("pageSize", pageSizeLow.getField());

        HistorySchema pageSizeTooHigh = baseRequest();
        pageSizeTooHigh.setPageSize(99);
        ApiErrorException pageSizeHigh = captureNormalizeError(pageSizeTooHigh);
        assertEquals("pageSize", pageSizeHigh.getField());
    }

    private ApiErrorException captureNormalizeError(HistorySchema request) {
        ApiErrorException exception = null;
        try {
            policy.normalize(request);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private HistorySchema baseRequest() {
        HistorySchema request = new HistorySchema();
        request.setScopeType("ACCOUNT");
        request.setScopeId(UUID.randomUUID().toString());
        return request;
    }

    private TransactionModuleConfig config(int maxPageSize) {
        TransactionModuleConfig config = new TransactionModuleConfig();
        config.setHistoryMaxPageSize(maxPageSize);
        return config;
    }
}

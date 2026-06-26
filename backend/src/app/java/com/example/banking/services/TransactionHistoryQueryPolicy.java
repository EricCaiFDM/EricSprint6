package com.example.banking.services;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.banking.api.transactions.schemas.HistorySchema;
import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.lib.errors.TransactionErrors;
import com.example.banking.models.TransactionType;

@Service
public class TransactionHistoryQueryPolicy {
    private final TransactionModuleConfig transactionModuleConfig;

    public TransactionHistoryQueryPolicy(TransactionModuleConfig transactionModuleConfig) {
        this.transactionModuleConfig = transactionModuleConfig;
    }

    public QueryPolicyResult normalize(HistorySchema request) {
        String scopeType = normalizeScopeType(request.getScopeType());
        String scopeId = normalizeScopeId(request.getScopeId());
        Instant startDateUtc = parseOptionalInstant(request.getStartDateUtc(), "startDateUtc");
        Instant endDateUtc = parseOptionalInstant(request.getEndDateUtc(), "endDateUtc");

        if (startDateUtc != null && endDateUtc != null && startDateUtc.isAfter(endDateUtc)) {
            throw TransactionErrors.validation("startDateUtc cannot be after endDateUtc", "startDateUtc");
        }

        TransactionType transactionType = parseOptionalTransactionType(request.getTransactionType());

        int page = request.getPage() == null ? 1 : request.getPage();
        int pageSize = request.getPageSize() == null ? 20 : request.getPageSize();
        int maxPageSize = Math.max(1, transactionModuleConfig.getHistoryMaxPageSize());
        if (page < 1) {
            throw TransactionErrors.validation("page must be greater than or equal to 1", "page");
        }
        if (pageSize < 1 || pageSize > maxPageSize) {
            throw TransactionErrors.validation(
                    "pageSize must be between 1 and " + maxPageSize,
                    "pageSize");
        }

        Pageable pageable = PageRequest.of(
                page - 1,
                pageSize,
                Sort.by(Sort.Order.desc("postedAtUtc"), Sort.Order.desc("transactionId")));

        return new QueryPolicyResult(scopeType, scopeId, startDateUtc, endDateUtc, transactionType, pageable);
    }

    private String normalizeScopeType(String scopeType) {
        if (scopeType == null || scopeType.isBlank()) {
            throw TransactionErrors.validation("scopeType is required", "scopeType");
        }
        String normalized = scopeType.trim().toUpperCase(Locale.ROOT);
        if (!"ACCOUNT".equals(normalized) && !"CUSTOMER".equals(normalized)) {
            throw TransactionErrors.validation("scopeType must be ACCOUNT or CUSTOMER", "scopeType");
        }
        return normalized;
    }

    private String normalizeScopeId(String scopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            throw TransactionErrors.validation("scopeId is required", "scopeId");
        }
        try {
            return UUID.fromString(scopeId.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw TransactionErrors.validation("scopeId must be a UUID", "scopeId");
        }
    }

    private Instant parseOptionalInstant(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException exception) {
            throw TransactionErrors.validation(field + " must be an ISO-8601 UTC timestamp", field);
        }
    }

    private TransactionType parseOptionalTransactionType(String transactionType) {
        if (transactionType == null || transactionType.isBlank()) {
            return null;
        }

        try {
            return TransactionType.valueOf(transactionType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw TransactionErrors.validation(
                    "transactionType must be one of DEPOSIT, WITHDRAWAL, TRANSFER_DEBIT, TRANSFER_CREDIT",
                    "transactionType");
        }
    }

    public record QueryPolicyResult(
            String scopeType,
            String scopeId,
            Instant startDateUtc,
            Instant endDateUtc,
            TransactionType transactionType,
            Pageable pageable) {
    }
}

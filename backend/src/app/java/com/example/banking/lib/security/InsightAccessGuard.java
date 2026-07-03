package com.example.banking.lib.security;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.insights.SpendingInsightQuery;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.lib.errors.InsightErrors;

@Service
public class InsightAccessGuard {
    private static final Duration MAX_PERIOD = Duration.ofDays(366);

    private final TransactionAccessPolicy transactionAccessPolicy;
    private final CustomerJpaRepository customerJpaRepository;

    public InsightAccessGuard(
            TransactionAccessPolicy transactionAccessPolicy,
            CustomerJpaRepository customerJpaRepository) {
        this.transactionAccessPolicy = transactionAccessPolicy;
        this.customerJpaRepository = customerJpaRepository;
    }

    public InsightScope resolveAndAuthorize(SpendingInsightQuery query, String actorUserId, String role) {
        String normalizedScopeType = normalizeScopeType(query.getScopeType());
        String normalizedScopeId = resolveScopeId(normalizedScopeType, query.getScopeId(), actorUserId);

        Instant periodEndUtc = parseOrDefault(query.getPeriodEndUtc(), Instant.now(), "periodEndUtc");
        Instant periodStartUtc = parseOrDefault(
                query.getPeriodStartUtc(),
                periodEndUtc.minus(Duration.ofDays(30)),
                "periodStartUtc");

        if (!periodStartUtc.isBefore(periodEndUtc)) {
            throw InsightErrors.validation("periodStartUtc must be earlier than periodEndUtc", "periodStartUtc");
        }

        if (Duration.between(periodStartUtc, periodEndUtc).compareTo(MAX_PERIOD) > 0) {
            throw InsightErrors.validation("Insight period cannot exceed 366 days", "periodEndUtc");
        }

        try {
            transactionAccessPolicy.enforceHistoryScope(normalizedScopeType, normalizedScopeId, role, actorUserId);
        } catch (ApiErrorException exception) {
            if ("TRANSACTION_FORBIDDEN".equals(exception.getCode())) {
                throw InsightErrors.forbidden();
            }
            if ("TRANSACTION_SCOPE_NOT_FOUND".equals(exception.getCode())) {
                throw InsightErrors.scopeNotFound("scopeId");
            }
            throw exception;
        }

        return new InsightScope(
                normalizedScopeType,
                normalizedScopeId,
                periodStartUtc,
                periodEndUtc,
                normalizeCategoryFilters(query.getCategoryFilters()));
    }

    private String normalizeScopeType(String scopeType) {
        if (scopeType == null || scopeType.isBlank()) {
            return "CUSTOMER";
        }

        String normalized = scopeType.trim().toUpperCase(Locale.ROOT);
        if (!"ACCOUNT".equals(normalized) && !"CUSTOMER".equals(normalized)) {
            throw InsightErrors.validation("scopeType must be ACCOUNT or CUSTOMER", "scopeType");
        }
        return normalized;
    }

    private String resolveScopeId(String scopeType, String scopeId, String actorUserId) {
        if (scopeId != null && !scopeId.isBlank()) {
            return normalizeUuid(scopeId, "scopeId");
        }

        if (!"CUSTOMER".equals(scopeType)) {
            throw InsightErrors.validation("scopeId is required for ACCOUNT scope", "scopeId");
        }

        if (actorUserId == null || actorUserId.isBlank()) {
            throw InsightErrors.scopeNotFound("scopeId");
        }

        return customerJpaRepository.findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(actorUserId)
                .or(() -> customerJpaRepository.findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(actorUserId))
                .map(customer -> customer.getCustomerId())
                .orElseThrow(() -> InsightErrors.scopeNotFound("scopeId"));
    }

    private String normalizeUuid(String value, String field) {
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw InsightErrors.validation(field + " must be a UUID", field);
        }
    }

    private Instant parseOrDefault(String value, Instant fallback, String field) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw InsightErrors.validation(field + " must be an ISO-8601 UTC datetime", field);
        }
    }

    private List<String> normalizeCategoryFilters(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return List.of(value.split(","))
                .stream()
                .map(filter -> filter == null ? "" : filter.trim().toUpperCase(Locale.ROOT))
                .filter(filter -> !filter.isBlank())
                .distinct()
                .toList();
    }

    public record InsightScope(
            String scopeType,
            String scopeId,
            Instant periodStartUtc,
            Instant periodEndUtc,
            List<String> categoryFilters) {
    }
}

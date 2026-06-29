package com.example.banking.lib.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.insights.SpendingInsightQuery;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.lib.errors.InsightErrors;
import com.example.banking.models.CustomerEntity;

class InsightAccessGuardTest {

    @Test
    void resolveAndAuthorizeUsesDefaultsAndResolvedCustomerScope() {
        TrackingPolicy policy = new TrackingPolicy();
        InsightAccessGuard guard = new InsightAccessGuard(policy, customerRepo("cust-1", "actor-1"));

        SpendingInsightQuery query = new SpendingInsightQuery();
        query.setScopeType("CUSTOMER");
        query.setCategoryFilters("food,food, fuel");

        Instant before = Instant.now();
        InsightAccessGuard.InsightScope scope = guard.resolveAndAuthorize(query, "actor-1", "customer");
        Instant after = Instant.now();

        assertEquals("CUSTOMER", scope.scopeType());
        assertEquals("cust-1", scope.scopeId());
        assertEquals(2, scope.categoryFilters().size());
        assertEquals("FOOD", scope.categoryFilters().get(0));
        assertEquals("FUEL", scope.categoryFilters().get(1));

        assertEquals("CUSTOMER", policy.scopeType);
        assertEquals("cust-1", policy.scopeId);
        assertEquals("customer", policy.role);
        assertEquals("actor-1", policy.actorUserId);

        assertEquals(30L, java.time.Duration.between(scope.periodStartUtc(), scope.periodEndUtc()).toDays());
        assertEquals(false, scope.periodEndUtc().isBefore(before));
        assertEquals(false, scope.periodEndUtc().isAfter(after.plusSeconds(5)));
    }

    @Test
    void resolveAndAuthorizeNormalizesExplicitScopeAndDates() {
        TrackingPolicy policy = new TrackingPolicy();
        InsightAccessGuard guard = new InsightAccessGuard(policy, customerRepo("unused", "other"));

        String accountId = UUID.randomUUID().toString();
        SpendingInsightQuery query = new SpendingInsightQuery();
        query.setScopeType("account");
        query.setScopeId(accountId);
        query.setPeriodStartUtc("2026-06-01T00:00:00Z");
        query.setPeriodEndUtc("2026-06-10T00:00:00Z");

        InsightAccessGuard.InsightScope scope = guard.resolveAndAuthorize(query, "actor-1", "ADMIN");

        assertEquals("ACCOUNT", scope.scopeType());
        assertEquals(accountId, scope.scopeId());
        assertEquals(Instant.parse("2026-06-01T00:00:00Z"), scope.periodStartUtc());
        assertEquals(Instant.parse("2026-06-10T00:00:00Z"), scope.periodEndUtc());
    }

    @Test
    void resolveAndAuthorizeValidatesScopeAndTimeWindow() {
        TrackingPolicy policy = new TrackingPolicy();
        InsightAccessGuard guard = new InsightAccessGuard(policy, customerRepo("cust-1", "actor-1"));

        SpendingInsightQuery badScopeType = new SpendingInsightQuery();
        badScopeType.setScopeType("branch");
        ApiErrorException scopeTypeError = assertThrows(
                ApiErrorException.class,
                () -> guard.resolveAndAuthorize(badScopeType, "actor-1", "CUSTOMER"));
        assertEquals("INSIGHT_VALIDATION_ERROR", scopeTypeError.getCode());

        SpendingInsightQuery accountMissingScopeId = new SpendingInsightQuery();
        accountMissingScopeId.setScopeType("ACCOUNT");
        ApiErrorException scopeIdError = assertThrows(
                ApiErrorException.class,
                () -> guard.resolveAndAuthorize(accountMissingScopeId, "actor-1", "CUSTOMER"));
        assertEquals("scopeId", scopeIdError.getField());

        SpendingInsightQuery badUuid = new SpendingInsightQuery();
        badUuid.setScopeType("ACCOUNT");
        badUuid.setScopeId("not-uuid");
        ApiErrorException uuidError = assertThrows(
                ApiErrorException.class,
                () -> guard.resolveAndAuthorize(badUuid, "actor-1", "CUSTOMER"));
        assertEquals("scopeId", uuidError.getField());

        SpendingInsightQuery badOrder = new SpendingInsightQuery();
        badOrder.setScopeType("CUSTOMER");
        badOrder.setPeriodStartUtc("2026-06-10T00:00:00Z");
        badOrder.setPeriodEndUtc("2026-06-01T00:00:00Z");
        ApiErrorException orderError = assertThrows(
                ApiErrorException.class,
                () -> guard.resolveAndAuthorize(badOrder, "actor-1", "CUSTOMER"));
        assertEquals("periodStartUtc", orderError.getField());

        SpendingInsightQuery tooLong = new SpendingInsightQuery();
        tooLong.setScopeType("CUSTOMER");
        tooLong.setPeriodStartUtc("2025-01-01T00:00:00Z");
        tooLong.setPeriodEndUtc("2026-06-01T00:00:00Z");
        ApiErrorException longError = assertThrows(
                ApiErrorException.class,
                () -> guard.resolveAndAuthorize(tooLong, "actor-1", "CUSTOMER"));
        assertEquals("periodEndUtc", longError.getField());
    }

    @Test
    void resolveAndAuthorizeMapsTransactionPolicyErrors() {
        InsightAccessGuard forbiddenGuard = new InsightAccessGuard(
                new ThrowingPolicy(new ApiErrorException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "TRANSACTION_FORBIDDEN",
                        "forbidden",
                        null)),
                customerRepo("cust-1", "actor-1"));

        SpendingInsightQuery query = new SpendingInsightQuery();
        query.setScopeType("CUSTOMER");

        ApiErrorException forbidden = assertThrows(
                ApiErrorException.class,
                () -> forbiddenGuard.resolveAndAuthorize(query, "actor-1", "CUSTOMER"));
        assertEquals(InsightErrors.forbidden().getCode(), forbidden.getCode());

        InsightAccessGuard notFoundGuard = new InsightAccessGuard(
                new ThrowingPolicy(new ApiErrorException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "TRANSACTION_SCOPE_NOT_FOUND",
                        "missing",
                        "scopeId")),
                customerRepo("cust-1", "actor-1"));

        ApiErrorException notFound = assertThrows(
                ApiErrorException.class,
                () -> notFoundGuard.resolveAndAuthorize(query, "actor-1", "CUSTOMER"));
        assertEquals(InsightErrors.scopeNotFound("scopeId").getCode(), notFound.getCode());
    }

    @Test
    void resolveAndAuthorizeFailsWhenCustomerCannotBeResolved() {
        InsightAccessGuard guard = new InsightAccessGuard(new TrackingPolicy(), emptyCustomerRepo());

        SpendingInsightQuery query = new SpendingInsightQuery();
        query.setScopeType("CUSTOMER");

        ApiErrorException missing = assertThrows(
                ApiErrorException.class,
                () -> guard.resolveAndAuthorize(query, "actor-1", "CUSTOMER"));
        assertEquals("INSIGHT_SCOPE_NOT_FOUND", missing.getCode());
    }

    private CustomerJpaRepository customerRepo(String customerId, String ownerUserId) {
        return (CustomerJpaRepository) Proxy.newProxyInstance(
                CustomerJpaRepository.class.getClassLoader(),
                new Class<?>[] { CustomerJpaRepository.class },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc".equals(name)
                            || "findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc".equals(name)) {
                        if (ownerUserId.equals(args[0])) {
                            CustomerEntity customer = new CustomerEntity();
                            customer.setCustomerId(customerId);
                            customer.setOwnerUserId(ownerUserId);
                            return Optional.of(customer);
                        }
                        return Optional.empty();
                    }
                    if ("findByCustomerIdAndDeletedAtIsNull".equals(name)) {
                        if (customerId.equals(args[0])) {
                            CustomerEntity customer = new CustomerEntity();
                            customer.setCustomerId(customerId);
                            customer.setOwnerUserId(ownerUserId);
                            return Optional.of(customer);
                        }
                        return Optional.empty();
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return Optional.empty();
                });
    }

    private CustomerJpaRepository emptyCustomerRepo() {
        return (CustomerJpaRepository) Proxy.newProxyInstance(
                CustomerJpaRepository.class.getClassLoader(),
                new Class<?>[] { CustomerJpaRepository.class },
                (proxy, method, args) -> {
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getName().startsWith("find")) {
                        return Optional.empty();
                    }
                    return null;
                });
    }

    private static class TrackingPolicy extends TransactionAccessPolicy {
        private String scopeType;
        private String scopeId;
        private String role;
        private String actorUserId;

        private TrackingPolicy() {
            super(null, null);
        }

        @Override
        public void enforceHistoryScope(String scopeType, String scopeId, String role, String actorUserId) {
            this.scopeType = scopeType;
            this.scopeId = scopeId;
            this.role = role;
            this.actorUserId = actorUserId;
        }
    }

    private static class ThrowingPolicy extends TransactionAccessPolicy {
        private final ApiErrorException exception;

        private ThrowingPolicy(ApiErrorException exception) {
            super(null, null);
            this.exception = exception;
        }

        @Override
        public void enforceHistoryScope(String scopeType, String scopeId, String role, String actorUserId) {
            throw exception;
        }
    }
}

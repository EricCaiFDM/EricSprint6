package com.example.banking.lib.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.CustomerEntity;

class TransactionAccessPolicyTest {

    private final Map<String, AccountEntity> accounts = new ConcurrentHashMap<>();
    private final Map<String, CustomerEntity> customers = new ConcurrentHashMap<>();
    private TransactionAccessPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new TransactionAccessPolicy(accountRepository(), customerRepository());
    }

    @Test
    void enforceMonetaryAndHistoryAccessRejectUnauthorizedRoles() {
        assertNull(captureMonetaryAccessError("ADMIN"));
        assertNull(captureMonetaryAccessError("customer"));
        assertNull(captureHistoryAccessError("ADMIN"));
        assertNull(captureHistoryAccessError("customer"));

        ApiErrorException postForbidden = captureMonetaryAccessError("auditor");
        assertNotNull(postForbidden);
        assertEquals("TRANSACTION_FORBIDDEN", postForbidden.getCode());

        ApiErrorException readForbidden = captureHistoryAccessError("guest");
        assertNotNull(readForbidden);
        assertEquals("TRANSACTION_FORBIDDEN", readForbidden.getCode());
    }

    @Test
    void requireAccountOperationScopeHandlesMissingAdminAndOwnership() {
        String accountId = "acc-1";
        AccountEntity account = account(accountId, "cust-1", "owner-1", "creator-1");
        accounts.put(accountId, account);

        AccountEntity adminResult = policy.requireAccountOperationScope(accountId, "ADMIN", "anyone", "post");
        assertNotNull(adminResult);

        AccountEntity ownerResult = policy.requireAccountOperationScope(accountId, "CUSTOMER", "owner-1", "post");
        assertNotNull(ownerResult);

        CustomerEntity customer = customer("cust-1", "owner-2", "creator-2");
        customers.put("cust-1", customer);
        AccountEntity inheritedOwnerResult = policy.requireAccountOperationScope(accountId, "CUSTOMER", "owner-2", "post");
        assertNotNull(inheritedOwnerResult);

        assertNull(captureRequireAccountOperationScopeError(accountId, "ADMIN", "anyone", "post"));

        ApiErrorException forbidden = captureRequireAccountOperationScopeError(accountId, "CUSTOMER", "outsider", "post");
        assertNotNull(forbidden);
        assertEquals("TRANSACTION_FORBIDDEN", forbidden.getCode());

        ApiErrorException missingAccount = captureRequireAccountOperationScopeError("missing", "ADMIN", "anyone", "post");
        assertNotNull(missingAccount);
        assertEquals("TRANSACTION_ACCOUNT_NOT_FOUND", missingAccount.getCode());
        assertEquals("accountId", missingAccount.getField());
    }

    @Test
    void enforceHistoryScopeForAccountHandlesAllOutcomes() {
        String accountId = "acc-10";
        AccountEntity account = account(accountId, "cust-10", "owner-10", "creator-10");
        accounts.put(accountId, account);

        policy.enforceHistoryScope("ACCOUNT", accountId, "ADMIN", "admin");
        policy.enforceHistoryScope("account", accountId, "CUSTOMER", "owner-10");

        CustomerEntity customer = customer("cust-10", "owner-customer", "creator-customer");
        customers.put("cust-10", customer);
        policy.enforceHistoryScope("ACCOUNT", accountId, "CUSTOMER", "owner-customer");

        assertNull(captureEnforceHistoryScopeError("ACCOUNT", accountId, "ADMIN", "admin"));

        ApiErrorException forbidden = captureEnforceHistoryScopeError("ACCOUNT", accountId, "CUSTOMER", "outsider");
        assertNotNull(forbidden);
        assertEquals("TRANSACTION_FORBIDDEN", forbidden.getCode());

        ApiErrorException missing = captureEnforceHistoryScopeError("ACCOUNT", "missing", "ADMIN", "admin");
        assertNotNull(missing);
        assertEquals("TRANSACTION_SCOPE_NOT_FOUND", missing.getCode());
        assertEquals("scopeId", missing.getField());
    }

    @Test
    void enforceHistoryScopeForCustomerHandlesAllOutcomes() {
        String customerId = "cust-50";
        customers.put(customerId, customer(customerId, "owner-50", "creator-50"));

        policy.enforceHistoryScope("CUSTOMER", customerId, "ADMIN", "admin");
        policy.enforceHistoryScope("CUSTOMER", customerId, "CUSTOMER", customerId);
        policy.enforceHistoryScope("CUSTOMER", customerId, "CUSTOMER", "owner-50");

        ApiErrorException forbidden = captureEnforceHistoryScopeError("CUSTOMER", customerId, "CUSTOMER", "outsider");
        assertNotNull(forbidden);
        assertEquals("TRANSACTION_FORBIDDEN", forbidden.getCode());

        ApiErrorException missing = captureEnforceHistoryScopeError("CUSTOMER", "missing", "ADMIN", "admin");
        assertNotNull(missing);
        assertEquals("TRANSACTION_SCOPE_NOT_FOUND", missing.getCode());

        ApiErrorException invalidScopeType = captureEnforceHistoryScopeError("BRANCH", customerId, "ADMIN", "admin");
        assertNotNull(invalidScopeType);
        assertEquals("TRANSACTION_VALIDATION_ERROR", invalidScopeType.getCode());
        assertEquals("scopeType", invalidScopeType.getField());
    }

    private ApiErrorException captureMonetaryAccessError(String role) {
        ApiErrorException exception = null;
        try {
            policy.enforceMonetaryAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureHistoryAccessError(String role) {
        ApiErrorException exception = null;
        try {
            policy.enforceHistoryAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureRequireAccountOperationScopeError(
            String accountId,
            String role,
            String actorUserId,
            String operation) {
        ApiErrorException exception = null;
        try {
            policy.requireAccountOperationScope(accountId, role, actorUserId, operation);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureEnforceHistoryScopeError(
            String scopeType,
            String scopeId,
            String role,
            String actorUserId) {
        ApiErrorException exception = null;
        try {
            policy.enforceHistoryScope(scopeType, scopeId, role, actorUserId);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    @Test
    void proxyRepositoriesCoverAllInvocationHandlerBranches() {
        AccountJpaRepository accountJpaRepository = accountRepository();
        CustomerJpaRepository customerJpaRepository = customerRepository();

        accounts.put("acc-proxy", account("acc-proxy", "cust-proxy", "owner-proxy", "creator-proxy"));
        customers.put("cust-proxy", customer("cust-proxy", "owner-proxy", "creator-proxy"));

        assertTrue(accountJpaRepository.findByAccountIdAndDeletedAtIsNull("acc-proxy").isPresent());
        assertTrue(accountJpaRepository.findByAccountIdAndDeletedAtIsNull("missing").isEmpty());
        assertFalse(accountJpaRepository.existsByCustomerId("cust-proxy"));
        assertNull(accountJpaRepository.findByDeletedAtIsNull());

        assertTrue(customerJpaRepository.findByCustomerIdAndDeletedAtIsNull("cust-proxy").isPresent());
        assertTrue(customerJpaRepository.findByCustomerIdAndDeletedAtIsNull("missing").isEmpty());

        assertTrue(customerJpaRepository
                .findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc("owner-proxy")
                .isPresent());
        assertTrue(customerJpaRepository
                .findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc("creator-proxy")
                .isPresent());
        assertTrue(customerJpaRepository
                .findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc("nobody")
                .isEmpty());

        assertFalse(customerJpaRepository.existsByPrimaryEmailIgnoreCaseAndDeletedAtIsNull("x@y.com"));
        assertTrue(customerJpaRepository.findById("missing").isEmpty());
    }

    private AccountJpaRepository accountRepository() {
        return (AccountJpaRepository) Proxy.newProxyInstance(
                AccountJpaRepository.class.getClassLoader(),
                new Class<?>[] { AccountJpaRepository.class },
                (proxy, method, args) -> {
                    if ("findByAccountIdAndDeletedAtIsNull".equals(method.getName())) {
                        return Optional.ofNullable(accounts.get((String) args[0]));
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return null;
                });
    }

    private CustomerJpaRepository customerRepository() {
        return (CustomerJpaRepository) Proxy.newProxyInstance(
                CustomerJpaRepository.class.getClassLoader(),
                new Class<?>[] { CustomerJpaRepository.class },
                (proxy, method, args) -> {
                    if ("findByCustomerIdAndDeletedAtIsNull".equals(method.getName())) {
                        return Optional.ofNullable(customers.get((String) args[0]));
                    }
                    if ("findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc".equals(method.getName())
                            || "findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc".equals(method.getName())) {
                        return customers.values().stream()
                                .filter(customer -> ((String) args[0]).equals(customer.getOwnerUserId())
                                        || ((String) args[0]).equals(customer.getCreatedByUserId()))
                                .findFirst();
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    return Optional.empty();
                });
    }

    private AccountEntity account(String accountId, String customerId, String ownerUserId, String createdByUserId) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setCustomerId(customerId);
        account.setOwnerUserId(ownerUserId);
        account.setCreatedByUserId(createdByUserId);
        return account;
    }

    private CustomerEntity customer(String customerId, String ownerUserId, String createdByUserId) {
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerId(customerId);
        customer.setOwnerUserId(ownerUserId);
        customer.setCreatedByUserId(createdByUserId);
        return customer;
    }
}

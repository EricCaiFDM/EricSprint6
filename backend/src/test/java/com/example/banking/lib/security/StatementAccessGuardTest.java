package com.example.banking.lib.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
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
import com.example.banking.models.statement.MonthlyStatement;

class StatementAccessGuardTest {

    private final Map<String, AccountEntity> accounts = new ConcurrentHashMap<>();
    private final Map<String, CustomerEntity> customers = new ConcurrentHashMap<>();

    private StatementAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new StatementAccessGuard(accountRepository(), customerRepository());
    }

    @Test
    void enforceAccessRejectsNullRoleAndAllowsNormalizedRoles() {
        guard.enforceGenerationAccess("ROLE_admin");
        guard.enforceRetrievalAccess(" customer ");

        ApiErrorException generationForbidden = captureGenerationAccessError(null);
        assertEquals("STATEMENT_FORBIDDEN", generationForbidden.getCode());

        ApiErrorException retrievalForbidden = captureRetrievalAccessError(null);
        assertEquals("STATEMENT_FORBIDDEN", retrievalForbidden.getCode());
    }

    @Test
    void requireAccountScopeUsesActorNormalizationAndOwnershipFallback() {
        AccountEntity account = account("acc-1", "cust-1", "owner-1", "creator-1");
        accounts.put("acc-1", account);

        AccountEntity ownerAccess = guard.requireAccountScope("acc-1", "CUSTOMER", " owner-1 ", "read");
        assertNotNull(ownerAccess);

        AccountEntity creatorAccess = guard.requireAccountScope("acc-1", "CUSTOMER", " creator-1 ", "read");
        assertNotNull(creatorAccess);

        CustomerEntity customer = customer("cust-1", "customer-owner", "customer-creator");
        customers.put("cust-1", customer);
        AccountEntity customerOwnerAccess = guard.requireAccountScope("acc-1", "CUSTOMER", "customer-owner", "read");
        assertNotNull(customerOwnerAccess);

        AccountEntity customerCreatorAccess = guard.requireAccountScope("acc-1", "CUSTOMER", "customer-creator", "read");
        assertNotNull(customerCreatorAccess);

        ApiErrorException forbiddenWithNullActor = captureRequireAccountScopeError("acc-1", "CUSTOMER", null, "read");
        assertEquals("STATEMENT_FORBIDDEN", forbiddenWithNullActor.getCode());
    }

    @Test
    void requireStatementScopeThrowsWhenStatementMissing() {
        ApiErrorException missing = captureRequireStatementScopeError(null, "CUSTOMER", "actor-1", "read");

        assertEquals("STATEMENT_NOT_FOUND", missing.getCode());
        assertEquals("statementId", missing.getField());

        MonthlyStatement statement = new MonthlyStatement();
        statement.setAccountId("acc-3");
        accounts.put("acc-3", account("acc-3", "cust-3", "owner-3", "creator-3"));

        guard.requireStatementScope(statement, "CUSTOMER", "owner-3", "read");
    }

    @Test
    void ownershipHelpersReturnFalseForNullInputs() {
        AccountEntity account = account("acc-2", "cust-2", "owner-2", "creator-2");
        CustomerEntity customer = customer("cust-2", "owner-2", "creator-2");

        assertEquals(false, invokePrivateBoolean(
                guard,
                "isAccountOwnedByActor",
                new Class<?>[] { AccountEntity.class, String.class },
                null,
                "actor"));

        assertEquals(false, invokePrivateBoolean(
                guard,
                "isAccountOwnedByActor",
                new Class<?>[] { AccountEntity.class, String.class },
                account,
                null));

        assertEquals(false, invokePrivateBoolean(
            guard,
            "isAccountOwnedByActor",
            new Class<?>[] { AccountEntity.class, String.class },
            account,
            "   "));

        assertTrue(invokePrivateBoolean(
            guard,
            "isAccountOwnedByActor",
            new Class<?>[] { AccountEntity.class, String.class },
            account,
            "creator-2"));

        assertEquals(false, invokePrivateBoolean(
                guard,
                "isCustomerOwnedByActor",
                new Class<?>[] { CustomerEntity.class, String.class },
                null,
                "actor"));

        assertEquals(false, invokePrivateBoolean(
                guard,
                "isCustomerOwnedByActor",
                new Class<?>[] { CustomerEntity.class, String.class },
                customer,
                null));

        assertEquals(false, invokePrivateBoolean(
                guard,
                "isCustomerOwnedByActor",
                new Class<?>[] { CustomerEntity.class, String.class },
                customer,
                "   "));

        assertTrue(invokePrivateBoolean(
            guard,
            "isCustomerOwnedByActor",
            new Class<?>[] { CustomerEntity.class, String.class },
            customer,
            "creator-2"));
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
                    if (Optional.class.equals(method.getReturnType())) {
                        return Optional.empty();
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
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (Optional.class.equals(method.getReturnType())) {
                        return Optional.empty();
                    }
                    return null;
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

    private boolean invokePrivateBoolean(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            Object result = method.invoke(target, args);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private ApiErrorException captureGenerationAccessError(String role) {
        ApiErrorException exception = null;
        try {
            guard.enforceGenerationAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureRetrievalAccessError(String role) {
        ApiErrorException exception = null;
        try {
            guard.enforceRetrievalAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureRequireAccountScopeError(
            String accountId,
            String role,
            String actorUserId,
            String operation) {
        ApiErrorException exception = null;
        try {
            guard.requireAccountScope(accountId, role, actorUserId, operation);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureRequireStatementScopeError(
            MonthlyStatement statement,
            String role,
            String actorUserId,
            String operation) {
        ApiErrorException exception = null;
        try {
            guard.requireStatementScope(statement, role, actorUserId, operation);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }
}
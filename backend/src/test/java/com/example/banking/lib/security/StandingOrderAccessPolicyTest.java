package com.example.banking.lib.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.CustomerEntity;
import com.example.banking.models.StandingOrderEntity;

class StandingOrderAccessPolicyTest {

    private AccountJpaRepository accountJpaRepository;
    private CustomerJpaRepository customerJpaRepository;
    private StandingOrderAccessPolicy policy;

    @BeforeEach
    void setUp() {
        accountJpaRepository = mock(AccountJpaRepository.class);
        customerJpaRepository = mock(CustomerJpaRepository.class);
        policy = new StandingOrderAccessPolicy(accountJpaRepository, customerJpaRepository);
    }

    @Test
    void enforceManageAccessAllowsAdminAndCustomerButRejectsOthers() {
        assertNull(captureManageAccessError("ADMIN"));
        assertNull(captureManageAccessError("customer"));

        ApiErrorException forbidden = captureManageAccessError("auditor");
        assertNotNull(forbidden);
        assertEquals("STANDING_ORDER_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void requireAccountScopeHandlesAdminOwnerInheritedOwnershipAndForbidden() {
        AccountEntity account = account("acc-1", "cust-1", "owner-1", "creator-1");
        when(accountJpaRepository.findByAccountIdAndDeletedAtIsNull("acc-1")).thenReturn(Optional.of(account));

        AccountEntity forAdmin = policy.requireAccountScope("acc-1", "ADMIN", "whoever", "accountId");
        assertNotNull(forAdmin);

        AccountEntity forOwner = policy.requireAccountScope("acc-1", "CUSTOMER", "owner-1", "accountId");
        assertNotNull(forOwner);

        CustomerEntity customer = customer("cust-1", "owner-2", "creator-2");
        when(customerJpaRepository.findByCustomerIdAndDeletedAtIsNull("cust-1")).thenReturn(Optional.of(customer));
        AccountEntity forCustomerOwner = policy.requireAccountScope("acc-1", "CUSTOMER", "owner-2", "accountId");
        assertNotNull(forCustomerOwner);

        assertNull(captureRequireAccountScopeError("acc-1", "ADMIN", "whoever", "accountId"));

        ApiErrorException forbidden = captureRequireAccountScopeError("acc-1", "CUSTOMER", "outsider", "accountId");
        assertNotNull(forbidden);
        assertEquals("STANDING_ORDER_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void requireAccountScopeThrowsWhenAccountMissing() {
        when(accountJpaRepository.findByAccountIdAndDeletedAtIsNull("missing")).thenReturn(Optional.empty());

        ApiErrorException exception = captureRequireAccountScopeError("missing", "CUSTOMER", "actor", "sourceAccountId");
        assertNotNull(exception);
        assertEquals("STANDING_ORDER_ACCOUNT_NOT_FOUND", exception.getCode());
        assertEquals("sourceAccountId", exception.getField());
    }

    @Test
    void requireStandingOrderScopeValidatesNullAndRespectsOwnership() {
        ApiErrorException notFound = captureRequireStandingOrderScopeError(null, "CUSTOMER", "actor", "read");
        assertNotNull(notFound);
        assertEquals("STANDING_ORDER_NOT_FOUND", notFound.getCode());

        StandingOrderEntity standingOrder = new StandingOrderEntity();
        standingOrder.setSourceAccountId("acc-10");

        policy.requireStandingOrderScope(standingOrder, "ADMIN", "actor", "read");

        AccountEntity sourceAccount = account("acc-10", "cust-10", "owner-10", "creator-10");
        when(accountJpaRepository.findByAccountIdAndDeletedAtIsNull("acc-10")).thenReturn(Optional.of(sourceAccount));

        policy.requireStandingOrderScope(standingOrder, "CUSTOMER", "owner-10", "read");

        CustomerEntity customer = customer("cust-10", "owner-from-customer", "creator-from-customer");
        when(customerJpaRepository.findByCustomerIdAndDeletedAtIsNull("cust-10")).thenReturn(Optional.of(customer));
        policy.requireStandingOrderScope(standingOrder, "CUSTOMER", "owner-from-customer", "read");

        assertNull(captureRequireStandingOrderScopeError(standingOrder, "ADMIN", "actor", "read"));

        ApiErrorException forbidden = captureRequireStandingOrderScopeError(standingOrder, "CUSTOMER", "outsider", "read");
        assertNotNull(forbidden);
        assertEquals("STANDING_ORDER_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void requireStandingOrderScopeThrowsWhenSourceAccountMissing() {
        StandingOrderEntity standingOrder = new StandingOrderEntity();
        standingOrder.setSourceAccountId("acc-missing");
        when(accountJpaRepository.findByAccountIdAndDeletedAtIsNull("acc-missing")).thenReturn(Optional.empty());

        ApiErrorException missingAccount = captureRequireStandingOrderScopeError(standingOrder, "CUSTOMER", "actor", "read");
        assertNotNull(missingAccount);
        assertEquals("STANDING_ORDER_ACCOUNT_NOT_FOUND", missingAccount.getCode());
        assertEquals("sourceAccountId", missingAccount.getField());
    }

    private ApiErrorException captureManageAccessError(String role) {
        ApiErrorException exception = null;
        try {
            policy.enforceManageAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureRequireAccountScopeError(
            String accountId,
            String role,
            String actorUserId,
            String accountField) {
        ApiErrorException exception = null;
        try {
            policy.requireAccountScope(accountId, role, actorUserId, accountField);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureRequireStandingOrderScopeError(
            StandingOrderEntity standingOrder,
            String role,
            String actorUserId,
            String operation) {
        ApiErrorException exception = null;
        try {
            policy.requireStandingOrderScope(standingOrder, role, actorUserId, operation);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
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

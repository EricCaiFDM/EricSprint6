package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.models.CustomerEntity;

class AccountAccessPolicyServiceTest {

    private final Map<String, CustomerEntity> customers = new ConcurrentHashMap<>();
    private AccountAccessPolicyService service;

    @BeforeEach
    void setUp() {
        service = new AccountAccessPolicyService(customerRepository());
    }

    @Test
    void roleBasedAccessMethodsAllowExpectedRolesAndRejectUnknownRole() {
        assertNull(captureCreateAccessError("customer"));
        assertNull(captureCreateAccessError("ADMIN"));
        assertNull(captureReadAccessError("ADMIN"));
        assertNull(captureReadAccessError("customer"));
        assertNull(captureListAccessError("customer"));
        assertNull(captureListAccessError("ADMIN"));
        assertNull(captureUpdateAccessError("ADMIN"));
        assertNull(captureUpdateAccessError("customer"));
        assertNull(captureDeleteAccessError("customer"));
        assertNull(captureDeleteAccessError("ADMIN"));

        ApiErrorException createForbidden = captureCreateAccessError("auditor");
        ApiErrorException readForbidden = captureReadAccessError(null);
        ApiErrorException listForbidden = captureListAccessError("guest");
        ApiErrorException updateForbidden = captureUpdateAccessError("guest");
        ApiErrorException deleteForbidden = captureDeleteAccessError("guest");

        assertEquals("ACCOUNT_FORBIDDEN", createForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", readForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", listForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", updateForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", deleteForbidden.getCode());
    }

    @Test
    void adminFinancialUpdateRequiresAdminRole() {
        assertNull(captureAdminFinancialUpdateError("ADMIN"));

        ApiErrorException forbidden = captureAdminFinancialUpdateError("CUSTOMER");

        assertEquals("ACCOUNT_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void enforceOwnershipIfRequiredCoversAdminDirectAndLegacyOwnership() {
        customers.put("cust-legacy", customer("cust-legacy", "owner-legacy", "creator-legacy"));

        assertNull(captureOwnershipError("ADMIN", "any-actor", "any-owner", "read"));
        assertNull(captureOwnershipError("CUSTOMER", "owner-direct", "owner-direct", "read"));
        assertNull(captureOwnershipError("CUSTOMER", "owner-legacy", "cust-legacy", "read"));
        assertNull(captureOwnershipError("CUSTOMER", "creator-legacy", "cust-legacy", "read"));

        ApiErrorException missingActorForbidden = captureOwnershipError("CUSTOMER", null, "owner-direct", "read");

        ApiErrorException blankActorForbidden = captureOwnershipError("CUSTOMER", "   ", "owner-direct", "read");

        ApiErrorException missingOwnerForbidden = captureOwnershipError("CUSTOMER", "actor", "   ", "read");

        ApiErrorException nullOwnerForbidden = captureOwnershipError("CUSTOMER", "actor", null, "read");

        ApiErrorException outsiderForbidden = captureOwnershipError("CUSTOMER", "outsider", "cust-legacy", "read");

        ApiErrorException nonCustomerRoleForbidden = captureOwnershipError("AUDITOR", "owner-direct", "owner-direct", "read");

        assertEquals("ACCOUNT_FORBIDDEN", missingActorForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", blankActorForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", missingOwnerForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", nullOwnerForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", outsiderForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", nonCustomerRoleForbidden.getCode());
    }

    @Test
    void enforceListScopeCoversAdminCustomerAndForbiddenPaths() {
        customers.put("cust-100", customer("cust-100", "owner-100", "creator-100"));

        assertNull(captureListScopeError("ADMIN", "any-actor", "cust-100"));
        assertNull(captureListScopeError("CUSTOMER", "cust-100", "cust-100"));
        assertNull(captureListScopeError("CUSTOMER", "owner-100", "cust-100"));
        assertNull(captureListScopeError("CUSTOMER", "creator-100", "cust-100"));

        ApiErrorException missingScopeForbidden = captureListScopeError("CUSTOMER", "owner-100", " ");

        ApiErrorException nullScopeForbidden = captureListScopeError("CUSTOMER", "owner-100", null);

        ApiErrorException blankActorForbidden = captureListScopeError("CUSTOMER", "   ", "cust-100");

        ApiErrorException nullActorForbidden = captureListScopeError("CUSTOMER", null, "cust-100");

        ApiErrorException outsiderForbidden = captureListScopeError("CUSTOMER", "outsider", "cust-100");

        ApiErrorException nonCustomerRoleForbidden = captureListScopeError("AUDITOR", "owner-100", "cust-100");

        assertEquals("ACCOUNT_FORBIDDEN", missingScopeForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", nullScopeForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", blankActorForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", nullActorForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", outsiderForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", nonCustomerRoleForbidden.getCode());
    }

    private ApiErrorException captureCreateAccessError(String role) {
        ApiErrorException exception = null;
        try {
            service.enforceCreateAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureReadAccessError(String role) {
        ApiErrorException exception = null;
        try {
            service.enforceReadAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureListAccessError(String role) {
        ApiErrorException exception = null;
        try {
            service.enforceListAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureUpdateAccessError(String role) {
        ApiErrorException exception = null;
        try {
            service.enforceUpdateAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureAdminFinancialUpdateError(String role) {
        ApiErrorException exception = null;
        try {
            service.enforceAdminFinancialUpdateAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureDeleteAccessError(String role) {
        ApiErrorException exception = null;
        try {
            service.enforceDeleteAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureOwnershipError(String role, String actorUserId, String ownerUserId, String operation) {
        ApiErrorException exception = null;
        try {
            service.enforceOwnershipIfRequired(role, actorUserId, ownerUserId, operation);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureListScopeError(String role, String actorUserId, String requestedCustomerId) {
        ApiErrorException exception = null;
        try {
            service.enforceListScope(role, actorUserId, requestedCustomerId);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    @Test
    void customerRepositoryProxyCoversAllInvocationHandlerBranches() {
        CustomerJpaRepository repository = customerRepository();
        customers.put("cust-proxy", customer("cust-proxy", "owner-proxy", "creator-proxy"));

        assertTrue(repository.findByCustomerIdAndDeletedAtIsNull("cust-proxy").isPresent());
        assertTrue(repository.findByCustomerIdAndDeletedAtIsNull("missing").isEmpty());

        assertFalse(repository.existsByPrimaryEmailIgnoreCaseAndDeletedAtIsNull("x@y.com"));

        assertTrue(repository.findById("missing").isEmpty());

        // For unsupported return types in this lightweight proxy, default branch returns null.
        assertNull(repository.findAll());
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

    private CustomerEntity customer(String customerId, String ownerUserId, String createdByUserId) {
        CustomerEntity customer = new CustomerEntity();
        customer.setCustomerId(customerId);
        customer.setOwnerUserId(ownerUserId);
        customer.setCreatedByUserId(createdByUserId);
        return customer;
    }
}
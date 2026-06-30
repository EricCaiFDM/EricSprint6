package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        service.enforceCreateAccess("customer");
        service.enforceCreateAccess("ADMIN");
        service.enforceReadAccess("ADMIN");
        service.enforceReadAccess("customer");
        service.enforceListAccess("customer");
        service.enforceListAccess("ADMIN");
        service.enforceUpdateAccess("ADMIN");
        service.enforceUpdateAccess("customer");
        service.enforceDeleteAccess("customer");
        service.enforceDeleteAccess("ADMIN");

        ApiErrorException createForbidden = assertThrows(ApiErrorException.class, () -> service.enforceCreateAccess("auditor"));
        ApiErrorException readForbidden = assertThrows(ApiErrorException.class, () -> service.enforceReadAccess(null));
        ApiErrorException listForbidden = assertThrows(ApiErrorException.class, () -> service.enforceListAccess("guest"));
        ApiErrorException updateForbidden = assertThrows(ApiErrorException.class, () -> service.enforceUpdateAccess("guest"));
        ApiErrorException deleteForbidden = assertThrows(ApiErrorException.class, () -> service.enforceDeleteAccess("guest"));

        assertEquals("ACCOUNT_FORBIDDEN", createForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", readForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", listForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", updateForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", deleteForbidden.getCode());
    }

    @Test
    void adminFinancialUpdateRequiresAdminRole() {
        service.enforceAdminFinancialUpdateAccess("ADMIN");

        ApiErrorException forbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceAdminFinancialUpdateAccess("CUSTOMER"));

        assertEquals("ACCOUNT_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void enforceOwnershipIfRequiredCoversAdminDirectAndLegacyOwnership() {
        customers.put("cust-legacy", customer("cust-legacy", "owner-legacy", "creator-legacy"));

        service.enforceOwnershipIfRequired("ADMIN", "any-actor", "any-owner", "read");
        service.enforceOwnershipIfRequired("CUSTOMER", "owner-direct", "owner-direct", "read");
        service.enforceOwnershipIfRequired("CUSTOMER", "owner-legacy", "cust-legacy", "read");
        service.enforceOwnershipIfRequired("CUSTOMER", "creator-legacy", "cust-legacy", "read");

        ApiErrorException missingActorForbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceOwnershipIfRequired("CUSTOMER", null, "owner-direct", "read"));

        ApiErrorException blankActorForbidden = assertThrows(
            ApiErrorException.class,
            () -> service.enforceOwnershipIfRequired("CUSTOMER", "   ", "owner-direct", "read"));

        ApiErrorException missingOwnerForbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceOwnershipIfRequired("CUSTOMER", "actor", "   ", "read"));

        ApiErrorException nullOwnerForbidden = assertThrows(
            ApiErrorException.class,
            () -> service.enforceOwnershipIfRequired("CUSTOMER", "actor", null, "read"));

        ApiErrorException outsiderForbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceOwnershipIfRequired("CUSTOMER", "outsider", "cust-legacy", "read"));

        ApiErrorException nonCustomerRoleForbidden = assertThrows(
            ApiErrorException.class,
            () -> service.enforceOwnershipIfRequired("AUDITOR", "owner-direct", "owner-direct", "read"));

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

        service.enforceListScope("ADMIN", "any-actor", "cust-100");
        service.enforceListScope("CUSTOMER", "cust-100", "cust-100");
        service.enforceListScope("CUSTOMER", "owner-100", "cust-100");
        service.enforceListScope("CUSTOMER", "creator-100", "cust-100");

        ApiErrorException missingScopeForbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceListScope("CUSTOMER", "owner-100", " "));

        ApiErrorException nullScopeForbidden = assertThrows(
            ApiErrorException.class,
            () -> service.enforceListScope("CUSTOMER", "owner-100", null));

        ApiErrorException blankActorForbidden = assertThrows(
            ApiErrorException.class,
            () -> service.enforceListScope("CUSTOMER", "   ", "cust-100"));

        ApiErrorException nullActorForbidden = assertThrows(
            ApiErrorException.class,
            () -> service.enforceListScope("CUSTOMER", null, "cust-100"));

        ApiErrorException outsiderForbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceListScope("CUSTOMER", "outsider", "cust-100"));

        ApiErrorException nonCustomerRoleForbidden = assertThrows(
            ApiErrorException.class,
            () -> service.enforceListScope("AUDITOR", "owner-100", "cust-100"));

        assertEquals("ACCOUNT_FORBIDDEN", missingScopeForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", nullScopeForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", blankActorForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", nullActorForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", outsiderForbidden.getCode());
        assertEquals("ACCOUNT_FORBIDDEN", nonCustomerRoleForbidden.getCode());
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
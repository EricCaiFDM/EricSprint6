package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;

class CustomerAccessPolicyServiceTest {

    private final CustomerAccessPolicyService service = new CustomerAccessPolicyService();

    @Test
    void createReadUpdateDeleteAllowAdminAndCustomerAndRejectOthers() {
        service.enforceCreateAccess("customer");
        service.enforceCreateAccess("ADMIN");
        service.enforceReadAccess("ADMIN");
        service.enforceReadAccess("customer");
        service.enforceUpdateAccess("customer");
        service.enforceUpdateAccess("ADMIN");
        service.enforceDeleteAccess("ADMIN");
        service.enforceDeleteAccess("customer");

        ApiErrorException createForbidden = assertThrows(ApiErrorException.class, () -> service.enforceCreateAccess("auditor"));
        ApiErrorException readForbidden = assertThrows(ApiErrorException.class, () -> service.enforceReadAccess(null));
        ApiErrorException updateForbidden = assertThrows(ApiErrorException.class, () -> service.enforceUpdateAccess("guest"));
        ApiErrorException deleteForbidden = assertThrows(ApiErrorException.class, () -> service.enforceDeleteAccess("guest"));

        assertEquals("CUSTOMER_FORBIDDEN", createForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", readForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", updateForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", deleteForbidden.getCode());
    }

    @Test
    void listAccessAllowsOnlyAdmin() {
        service.enforceListAccess("ADMIN");

        ApiErrorException customerForbidden = assertThrows(ApiErrorException.class, () -> service.enforceListAccess("CUSTOMER"));
        ApiErrorException nullForbidden = assertThrows(ApiErrorException.class, () -> service.enforceListAccess(null));

        assertEquals("CUSTOMER_FORBIDDEN", customerForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", nullForbidden.getCode());
    }

    @Test
    void ownershipEnforcementCoversAdminOwnedCreatedByAndForbiddenPaths() {
        service.enforceOwnershipIfRequired("ADMIN", "any", "owner-1", "creator-1", "read");
        service.enforceOwnershipIfRequired("CUSTOMER", "owner-1", "owner-1", "creator-1", "read");
        service.enforceOwnershipIfRequired("CUSTOMER", "creator-1", "owner-1", "creator-1", "read");

        ApiErrorException missingActorForbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceOwnershipIfRequired("CUSTOMER", null, "owner-1", "creator-1", "read"));

        ApiErrorException outsiderForbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceOwnershipIfRequired("CUSTOMER", "outsider", "owner-1", "creator-1", "read"));

        ApiErrorException nullRoleForbidden = assertThrows(
                ApiErrorException.class,
                () -> service.enforceOwnershipIfRequired(null, "owner-1", "owner-1", "creator-1", "read"));

        assertEquals("CUSTOMER_FORBIDDEN", missingActorForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", outsiderForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", nullRoleForbidden.getCode());
    }
}
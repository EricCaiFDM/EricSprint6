package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;

class CustomerAccessPolicyServiceTest {

    private final CustomerAccessPolicyService service = new CustomerAccessPolicyService();

    @Test
    void createReadUpdateDeleteAllowAdminAndCustomerAndRejectOthers() {
        assertNull(captureCreateAccessError("customer"));
        assertNull(captureCreateAccessError("ADMIN"));
        assertNull(captureReadAccessError("ADMIN"));
        assertNull(captureReadAccessError("customer"));
        assertNull(captureUpdateAccessError("customer"));
        assertNull(captureUpdateAccessError("ADMIN"));
        assertNull(captureDeleteAccessError("ADMIN"));
        assertNull(captureDeleteAccessError("customer"));

        ApiErrorException createForbidden = captureCreateAccessError("auditor");
        ApiErrorException readForbidden = captureReadAccessError(null);
        ApiErrorException updateForbidden = captureUpdateAccessError("guest");
        ApiErrorException deleteForbidden = captureDeleteAccessError("guest");

        assertEquals("CUSTOMER_FORBIDDEN", createForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", readForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", updateForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", deleteForbidden.getCode());
    }

    @Test
    void listAccessAllowsOnlyAdmin() {
        assertNull(captureListAccessError("ADMIN"));

        ApiErrorException customerForbidden = captureListAccessError("CUSTOMER");
        ApiErrorException nullForbidden = captureListAccessError(null);

        assertEquals("CUSTOMER_FORBIDDEN", customerForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", nullForbidden.getCode());
    }

    @Test
    void ownershipEnforcementCoversAdminOwnedCreatedByAndForbiddenPaths() {
        assertNull(captureOwnershipError("ADMIN", "any", "owner-1", "creator-1", "read"));
        assertNull(captureOwnershipError("CUSTOMER", "owner-1", "owner-1", "creator-1", "read"));
        assertNull(captureOwnershipError("CUSTOMER", "creator-1", "owner-1", "creator-1", "read"));

        ApiErrorException missingActorForbidden = captureOwnershipError("CUSTOMER", null, "owner-1", "creator-1", "read");

        ApiErrorException outsiderForbidden = captureOwnershipError("CUSTOMER", "outsider", "owner-1", "creator-1", "read");

        ApiErrorException nullRoleForbidden = captureOwnershipError(null, "owner-1", "owner-1", "creator-1", "read");

        assertEquals("CUSTOMER_FORBIDDEN", missingActorForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", outsiderForbidden.getCode());
        assertEquals("CUSTOMER_FORBIDDEN", nullRoleForbidden.getCode());
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

    private ApiErrorException captureUpdateAccessError(String role) {
        ApiErrorException exception = null;
        try {
            service.enforceUpdateAccess(role);
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

    private ApiErrorException captureListAccessError(String role) {
        ApiErrorException exception = null;
        try {
            service.enforceListAccess(role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureOwnershipError(
            String role,
            String actorUserId,
            String ownerUserId,
            String createdByUserId,
            String operation) {
        ApiErrorException exception = null;
        try {
            service.enforceOwnershipIfRequired(role, actorUserId, ownerUserId, createdByUserId, operation);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }
}
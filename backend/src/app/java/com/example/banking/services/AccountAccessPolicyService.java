package com.example.banking.services;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.CustomerJpaRepository;

@Service
public class AccountAccessPolicyService {
    private final CustomerJpaRepository customerJpaRepository;

    public AccountAccessPolicyService(CustomerJpaRepository customerJpaRepository) {
        this.customerJpaRepository = customerJpaRepository;
    }

    public void enforceCreateAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("CUSTOMER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("create");
    }

    public void enforceReadAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("CUSTOMER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("read");
    }

    public void enforceListAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("CUSTOMER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("list");
    }

    public void enforceUpdateAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("CUSTOMER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("update");
    }

    public void enforceAdminFinancialUpdateAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("update");
    }

    public void enforceDeleteAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("CUSTOMER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("delete");
    }

    public void enforceOwnershipIfRequired(String role, String actorUserId, String ownerUserId, String operation) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole)) {
            return;
        }
        if ("CUSTOMER".equals(normalizedRole) && isOwnedByActor(actorUserId, ownerUserId)) {
            return;
        }
        throw forbidden(operation);
    }

    public void enforceListScope(String role, String actorUserId, String requestedCustomerId) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole)) {
            return;
        }
        if ("CUSTOMER".equals(normalizedRole) && isCustomerOwnedByActor(actorUserId, requestedCustomerId)) {
            return;
        }
        throw forbidden("list");
    }

    private boolean isOwnedByActor(String actorUserId, String ownerUserId) {
        if (actorUserId == null || actorUserId.isBlank() || ownerUserId == null || ownerUserId.isBlank()) {
            return false;
        }

        if (actorUserId.equals(ownerUserId)) {
            return true;
        }

        // Backward compatibility for legacy rows where owner_user_id stored customer_id.
        return isCustomerOwnedByActor(actorUserId, ownerUserId);
    }

    private boolean isCustomerOwnedByActor(String actorUserId, String customerId) {
        if (actorUserId == null || actorUserId.isBlank() || customerId == null || customerId.isBlank()) {
            return false;
        }

        if (actorUserId.equals(customerId)) {
            return true;
        }

        return customerJpaRepository.findByCustomerIdAndDeletedAtIsNull(customerId)
            .map(customer -> actorUserId.equals(customer.getOwnerUserId())
                || actorUserId.equals(customer.getCreatedByUserId()))
                .orElse(false);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.toUpperCase(Locale.ROOT);
    }

    private ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_FORBIDDEN",
                "Insufficient privileges to " + operation + " account",
                null);
    }
}

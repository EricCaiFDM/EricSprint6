package com.example.banking.services;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;

@Service
public class AccountAccessPolicyService {
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
        if ("CUSTOMER".equals(normalizedRole) && actorUserId != null && actorUserId.equals(ownerUserId)) {
            return;
        }
        throw forbidden(operation);
    }

    public void enforceListScope(String role, String actorUserId, String requestedCustomerId) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole)) {
            return;
        }
        if ("CUSTOMER".equals(normalizedRole) && actorUserId != null && actorUserId.equals(requestedCustomerId)) {
            return;
        }
        throw forbidden("list");
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

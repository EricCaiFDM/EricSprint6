package com.example.banking.services;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;

@Service
public class CustomerAccessPolicyService {
    public void enforceCreateAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("CUSTOMER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("create");
    }

    public void enforceListAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("list");
    }

    public void enforceReadAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("CUSTOMER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        throw forbidden("read");
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

    public void enforceOwnershipIfRequired(
            String role,
            String actorUserId,
            String ownerUserId,
            String createdByUserId,
            String operation) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole)) {
            return;
        }
        if ("CUSTOMER".equals(normalizedRole)
                && actorUserId != null
                && (actorUserId.equals(ownerUserId) || actorUserId.equals(createdByUserId))) {
            return;
        }
        throw forbidden(operation);
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
                "CUSTOMER_FORBIDDEN",
                "Insufficient privileges to " + operation + " customer profile",
                null);
    }
}

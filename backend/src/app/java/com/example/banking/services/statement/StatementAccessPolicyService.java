package com.example.banking.services.statement;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.models.statement.StatementAccessPolicy;

@Service
public class StatementAccessPolicyService {
    private final CustomerJpaRepository customerJpaRepository;

    public StatementAccessPolicyService(CustomerJpaRepository customerJpaRepository) {
        this.customerJpaRepository = customerJpaRepository;
    }

    public StatementAccessPolicy resolve(String actorUserId, String role) {
        String normalizedUserId = normalizeActor(actorUserId);
        String normalizedRole = normalizeRole(role);

        if ("ADMIN".equals(normalizedRole)) {
            return new StatementAccessPolicy(normalizedUserId, normalizedRole, null);
        }

        String customerScopeId = customerJpaRepository
                .findFirstByOwnerUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(normalizedUserId)
                .or(() -> customerJpaRepository
                        .findFirstByCreatedByUserIdAndDeletedAtIsNullOrderByCreatedAtUtcDesc(normalizedUserId))
                .map(customer -> customer.getCustomerId())
                .orElse(null);

        return new StatementAccessPolicy(normalizedUserId, normalizedRole, customerScopeId);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId.trim();
    }
}

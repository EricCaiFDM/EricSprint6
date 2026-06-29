package com.example.banking.lib.security;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.lib.errors.NotificationErrors;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.CustomerEntity;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationRecipientScopeType;

@Service
public class NotificationAccessPolicy {
    private final AccountJpaRepository accountJpaRepository;
    private final CustomerJpaRepository customerJpaRepository;

    public NotificationAccessPolicy(
            AccountJpaRepository accountJpaRepository,
            CustomerJpaRepository customerJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
        this.customerJpaRepository = customerJpaRepository;
    }

    public void enforceTriggerAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole) || "CUSTOMER".equals(normalizedRole)) {
            return;
        }
        throw NotificationErrors.forbidden("trigger");
    }

    public void requireRecipientScope(
            NotificationRecipientScopeType scopeType,
            String recipientScopeId,
            String role,
            String actorUserId,
            String operation) {
        if (scopeType == null || recipientScopeId == null || recipientScopeId.isBlank()) {
            throw NotificationErrors.validation("recipientScopeId is required", "recipientScopeId");
        }

        if (isAdmin(role)) {
            ensureScopeExists(scopeType, recipientScopeId);
            return;
        }

        String actor = normalizeActor(actorUserId);
        switch (scopeType) {
            case CUSTOMER -> {
                CustomerEntity customer = customerJpaRepository.findByCustomerIdAndDeletedAtIsNull(recipientScopeId)
                        .orElseThrow(() -> NotificationErrors.scopeNotFound("recipientScopeId"));
                if (!isCustomerOwnedByActor(customer, actor)) {
                    throw NotificationErrors.forbidden(operation);
                }
            }
            case ACCOUNT -> {
                AccountEntity account = accountJpaRepository.findByAccountIdAndDeletedAtIsNull(recipientScopeId)
                        .orElseThrow(() -> NotificationErrors.scopeNotFound("recipientScopeId"));
                if (!isAccountOwnedByActor(account, actor)) {
                    throw NotificationErrors.forbidden(operation);
                }
            }
            case ADMIN -> throw NotificationErrors.forbidden(operation);
            default -> throw NotificationErrors.validation("recipientScopeType is invalid", "recipientScopeType");
        }
    }

    public void requireEventScope(NotificationEventEntity event, String role, String actorUserId, String operation) {
        if (event == null) {
            throw NotificationErrors.notFound(null);
        }
        requireRecipientScope(event.getRecipientScopeType(), event.getRecipientScopeId(), role, actorUserId, operation);
    }

    private void ensureScopeExists(NotificationRecipientScopeType scopeType, String recipientScopeId) {
        switch (scopeType) {
            case CUSTOMER -> customerJpaRepository.findByCustomerIdAndDeletedAtIsNull(recipientScopeId)
                    .orElseThrow(() -> NotificationErrors.scopeNotFound("recipientScopeId"));
            case ACCOUNT -> accountJpaRepository.findByAccountIdAndDeletedAtIsNull(recipientScopeId)
                    .orElseThrow(() -> NotificationErrors.scopeNotFound("recipientScopeId"));
            case ADMIN -> {
                // Admin scope is virtual and does not map to persisted resource in this service.
            }
            default -> throw NotificationErrors.validation("recipientScopeType is invalid", "recipientScopeType");
        }
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(normalizeRole(role));
    }

    private boolean isCustomerOwnedByActor(CustomerEntity customer, String actorUserId) {
        if (customer == null || actorUserId == null || actorUserId.isBlank()) {
            return false;
        }
        return actorUserId.equals(customer.getOwnerUserId()) || actorUserId.equals(customer.getCreatedByUserId());
    }

    private boolean isAccountOwnedByActor(AccountEntity account, String actorUserId) {
        if (account == null || actorUserId == null || actorUserId.isBlank()) {
            return false;
        }

        if (actorUserId.equals(account.getOwnerUserId()) || actorUserId.equals(account.getCreatedByUserId())) {
            return true;
        }

        return customerJpaRepository.findByCustomerIdAndDeletedAtIsNull(account.getCustomerId())
                .map(customer -> isCustomerOwnedByActor(customer, actorUserId))
                .orElse(false);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null) {
            return "";
        }
        return actorUserId.trim();
    }
}

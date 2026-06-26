package com.example.banking.lib.security;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.lib.errors.TransactionErrors;
import com.example.banking.models.AccountEntity;

@Service
public class TransactionAccessPolicy {
    private final AccountJpaRepository accountJpaRepository;
    private final CustomerJpaRepository customerJpaRepository;

    public TransactionAccessPolicy(AccountJpaRepository accountJpaRepository, CustomerJpaRepository customerJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
        this.customerJpaRepository = customerJpaRepository;
    }

    public void enforceMonetaryAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole) || "CUSTOMER".equals(normalizedRole)) {
            return;
        }
        throw TransactionErrors.forbidden("post");
    }

    public void enforceHistoryAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole) || "CUSTOMER".equals(normalizedRole)) {
            return;
        }
        throw TransactionErrors.forbidden("read");
    }

    public AccountEntity requireAccountOperationScope(String accountId, String role, String actorUserId, String operation) {
        enforceMonetaryAccess(role);
        AccountEntity account = accountJpaRepository.findByAccountIdAndDeletedAtIsNull(accountId)
                .orElseThrow(() -> TransactionErrors.accountNotFound("accountId"));

        if ("ADMIN".equals(normalizeRole(role))) {
            return account;
        }

        if (isAccountInCustomerScope(actorUserId, account)) {
            return account;
        }

        throw TransactionErrors.forbidden(operation);
    }

    public void enforceHistoryScope(String scopeType, String scopeId, String role, String actorUserId) {
        enforceHistoryAccess(role);
        String normalizedScopeType = normalizeScopeType(scopeType);

        if ("ACCOUNT".equals(normalizedScopeType)) {
            AccountEntity account = accountJpaRepository.findByAccountIdAndDeletedAtIsNull(scopeId)
                    .orElseThrow(() -> TransactionErrors.scopeNotFound("scopeId", "Requested account scope was not found"));
            if ("ADMIN".equals(normalizeRole(role)) || isAccountInCustomerScope(actorUserId, account)) {
                return;
            }
            throw TransactionErrors.forbidden("read");
        }

        if ("CUSTOMER".equals(normalizedScopeType)) {
            boolean customerExists = customerJpaRepository.findByCustomerIdAndDeletedAtIsNull(scopeId).isPresent();
            if (!customerExists) {
                throw TransactionErrors.scopeNotFound("scopeId", "Requested customer scope was not found");
            }
            if ("ADMIN".equals(normalizeRole(role)) || isCustomerInScope(actorUserId, scopeId)) {
                return;
            }
            throw TransactionErrors.forbidden("read");
        }

        throw TransactionErrors.validation("scopeType must be ACCOUNT or CUSTOMER", "scopeType");
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeScopeType(String scopeType) {
        if (scopeType == null) {
            return "";
        }
        return scopeType.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isAccountInCustomerScope(String actorUserId, AccountEntity account) {
        if (actorUserId == null || actorUserId.isBlank() || account == null) {
            return false;
        }
        if (actorUserId.equals(account.getOwnerUserId()) || actorUserId.equals(account.getCreatedByUserId())) {
            return true;
        }
        return isCustomerInScope(actorUserId, account.getCustomerId());
    }

    private boolean isCustomerInScope(String actorUserId, String customerId) {
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
}

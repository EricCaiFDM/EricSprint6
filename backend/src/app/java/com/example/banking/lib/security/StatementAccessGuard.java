package com.example.banking.lib.security;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.lib.errors.StatementErrors;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.CustomerEntity;
import com.example.banking.models.statement.MonthlyStatement;

@Service
public class StatementAccessGuard {
    private final AccountJpaRepository accountJpaRepository;
    private final CustomerJpaRepository customerJpaRepository;

    public StatementAccessGuard(
            AccountJpaRepository accountJpaRepository,
            CustomerJpaRepository customerJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
        this.customerJpaRepository = customerJpaRepository;
    }

    public void enforceGenerationAccess(String role) {
        if (isAdmin(role) || isCustomer(role)) {
            return;
        }
        throw StatementErrors.forbidden("generate");
    }

    public void enforceRetrievalAccess(String role) {
        if (isAdmin(role) || isCustomer(role)) {
            return;
        }
        throw StatementErrors.forbidden("read");
    }

    public AccountEntity requireAccountScope(String accountId, String role, String actorUserId, String operation) {
        AccountEntity account = accountJpaRepository.findByAccountIdAndDeletedAtIsNull(accountId)
                .orElseThrow(() -> StatementErrors.accountNotFound("accountId"));

        if (isAdmin(role) || isAccountOwnedByActor(account, normalizeActor(actorUserId))) {
            return account;
        }

        throw StatementErrors.forbidden(operation);
    }

    public void requireStatementScope(MonthlyStatement statement, String role, String actorUserId, String operation) {
        if (statement == null) {
            throw StatementErrors.notFound(null);
        }

        requireAccountScope(statement.getAccountId(), role, actorUserId, operation);
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

    private boolean isCustomerOwnedByActor(CustomerEntity customer, String actorUserId) {
        if (customer == null || actorUserId == null || actorUserId.isBlank()) {
            return false;
        }

        return actorUserId.equals(customer.getOwnerUserId()) || actorUserId.equals(customer.getCreatedByUserId());
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(normalizeRole(role));
    }

    private boolean isCustomer(String role) {
        return "CUSTOMER".equals(normalizeRole(role));
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null) {
            return "";
        }
        return actorUserId.trim();
    }
}

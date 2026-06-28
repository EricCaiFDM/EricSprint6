package com.example.banking.lib.security;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.CustomerJpaRepository;
import com.example.banking.lib.errors.StandingOrderErrors;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.StandingOrderEntity;

@Service
public class StandingOrderAccessPolicy {
    private final AccountJpaRepository accountJpaRepository;
    private final CustomerJpaRepository customerJpaRepository;

    public StandingOrderAccessPolicy(AccountJpaRepository accountJpaRepository, CustomerJpaRepository customerJpaRepository) {
        this.accountJpaRepository = accountJpaRepository;
        this.customerJpaRepository = customerJpaRepository;
    }

    public void enforceManageAccess(String role) {
        String normalizedRole = normalizeRole(role);
        if ("ADMIN".equals(normalizedRole) || "CUSTOMER".equals(normalizedRole)) {
            return;
        }
        throw StandingOrderErrors.forbidden("manage");
    }

    public AccountEntity requireAccountScope(String accountId, String role, String actorUserId, String field) {
        AccountEntity account = accountJpaRepository.findByAccountIdAndDeletedAtIsNull(accountId)
                .orElseThrow(() -> StandingOrderErrors.accountNotFound(field));
        if ("ADMIN".equals(normalizeRole(role)) || isAccountOwnedByActor(actorUserId, account)) {
            return account;
        }
        throw StandingOrderErrors.forbidden("manage");
    }

    public void requireStandingOrderScope(StandingOrderEntity standingOrder, String role, String actorUserId, String operation) {
        if (standingOrder == null) {
            throw StandingOrderErrors.notFound(null);
        }

        if ("ADMIN".equals(normalizeRole(role))) {
            return;
        }

        AccountEntity sourceAccount = accountJpaRepository.findByAccountIdAndDeletedAtIsNull(standingOrder.getSourceAccountId())
                .orElseThrow(() -> StandingOrderErrors.accountNotFound("sourceAccountId"));

        if (isAccountOwnedByActor(actorUserId, sourceAccount)) {
            return;
        }

        throw StandingOrderErrors.forbidden(operation);
    }

    private boolean isAccountOwnedByActor(String actorUserId, AccountEntity account) {
        if (actorUserId == null || actorUserId.isBlank() || account == null) {
            return false;
        }

        if (actorUserId.equals(account.getOwnerUserId()) || actorUserId.equals(account.getCreatedByUserId())) {
            return true;
        }

        return customerJpaRepository.findByCustomerIdAndDeletedAtIsNull(account.getCustomerId())
                .map(customer -> actorUserId.equals(customer.getOwnerUserId()) || actorUserId.equals(customer.getCreatedByUserId()))
                .orElse(false);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }
}

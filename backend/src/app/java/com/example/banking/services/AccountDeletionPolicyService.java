package com.example.banking.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.AccountDeletionPolicyCheckJpaRepository;
import com.example.banking.models.AccountDeletionPolicyCheckEntity;
import com.example.banking.models.AccountEntity;

@Service
public class AccountDeletionPolicyService {
    private final AccountDeletionPolicyCheckJpaRepository checkRepository;

    public AccountDeletionPolicyService(AccountDeletionPolicyCheckJpaRepository checkRepository) {
        this.checkRepository = checkRepository;
    }

    public DeletionDecision evaluateDeletion(AccountEntity account) {
        boolean dependencyBlocker = account.getBalance() != null && account.getBalance().signum() != 0;
        boolean retentionBlocker = false;

        List<String> reasons = new ArrayList<>();
        if (dependencyBlocker) {
            reasons.add("BALANCE_NOT_ZERO");
        }
        if (retentionBlocker) {
            reasons.add("RETENTION_BLOCKER");
        }

        DeletionDecision decision = reasons.isEmpty()
                ? new DeletionDecision(false, false, List.of(), "ALLOW_DELETE")
                : new DeletionDecision(dependencyBlocker, retentionBlocker, reasons, "BLOCK_DELETE");

        persistDecision(account.getAccountId(), decision);
        return decision;
    }

    public void enforceDeletionAllowed(DeletionDecision decision) {
        if ("BLOCK_DELETE".equals(decision.decision())) {
            throw new ApiErrorException(
                    HttpStatus.CONFLICT,
                    "ACCOUNT_DELETE_BLOCKED",
                    "Account deletion blocked by policy",
                    null);
        }
    }

    private void persistDecision(String accountId, DeletionDecision decision) {
        AccountDeletionPolicyCheckEntity check = new AccountDeletionPolicyCheckEntity();
        check.setAccountId(accountId);
        check.setEvaluatedAtUtc(Instant.now());
        check.setHasDependencyBlocker(decision.hasDependencyBlocker());
        check.setHasRetentionBlocker(decision.hasRetentionBlocker());
        check.setBlockerReasons(String.join(",", decision.blockerReasons()));
        check.setDecision(decision.decision());
        checkRepository.save(check);
    }

    public record DeletionDecision(
            boolean hasDependencyBlocker,
            boolean hasRetentionBlocker,
            List<String> blockerReasons,
            String decision) {
    }
}

package com.example.banking.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.AccountJpaRepository;
import com.example.banking.lib.DeletionPolicyCheckJpaRepository;
import com.example.banking.models.CustomerEntity;
import com.example.banking.models.DeletionPolicyCheckEntity;

@Service
public class CustomerDeletionPolicyService {
    private final AccountJpaRepository accountRepository;
    private final DeletionPolicyCheckJpaRepository checkRepository;

    public CustomerDeletionPolicyService(
            AccountJpaRepository accountRepository,
            DeletionPolicyCheckJpaRepository checkRepository) {
        this.accountRepository = accountRepository;
        this.checkRepository = checkRepository;
    }

    public DeletionDecision evaluateDeletion(CustomerEntity customer) {
        boolean dependencyBlocker = accountRepository.existsByCustomerId(customer.getCustomerId());
        boolean retentionBlocker = false;

        List<String> reasons = new ArrayList<>();
        if (dependencyBlocker) {
            reasons.add("DEPENDENCY_BLOCKER");
        }
        if (retentionBlocker) {
            reasons.add("RETENTION_BLOCKER");
        }

        DeletionDecision decision = reasons.isEmpty()
                ? new DeletionDecision(false, false, List.of(), "ALLOW_DELETE")
                : new DeletionDecision(dependencyBlocker, retentionBlocker, reasons, "BLOCK_DELETE");

        persistDecision(customer.getCustomerId(), decision);
        return decision;
    }

    public void enforceDeletionAllowed(DeletionDecision decision) {
        if ("BLOCK_DELETE".equals(decision.decision())) {
            throw new ApiErrorException(
                    HttpStatus.CONFLICT,
                    "CUSTOMER_DELETE_BLOCKED",
                    "Deletion blocked by policy",
                    null);
        }
    }

    private void persistDecision(String customerId, DeletionDecision decision) {
        DeletionPolicyCheckEntity check = new DeletionPolicyCheckEntity();
        check.setCustomerId(customerId);
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

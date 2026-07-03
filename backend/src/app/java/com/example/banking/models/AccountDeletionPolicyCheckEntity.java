package com.example.banking.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_deletion_policy_checks")
public class AccountDeletionPolicyCheckEntity {
    @Id
    @Column(name = "check_id", nullable = false, length = 36)
    private String checkId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "evaluated_at_utc", nullable = false)
    private Instant evaluatedAtUtc;

    @Column(name = "has_dependency_blocker", nullable = false)
    private boolean hasDependencyBlocker;

    @Column(name = "has_retention_blocker", nullable = false)
    private boolean hasRetentionBlocker;

    @Column(name = "blocker_reasons", columnDefinition = "TEXT")
    private String blockerReasons;

    @Column(name = "decision", nullable = false, length = 16)
    private String decision;

    public AccountDeletionPolicyCheckEntity() {
        this.checkId = UUID.randomUUID().toString();
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Instant getEvaluatedAtUtc() {
        return evaluatedAtUtc;
    }

    public void setEvaluatedAtUtc(Instant evaluatedAtUtc) {
        this.evaluatedAtUtc = evaluatedAtUtc;
    }

    public boolean isHasDependencyBlocker() {
        return hasDependencyBlocker;
    }

    public void setHasDependencyBlocker(boolean hasDependencyBlocker) {
        this.hasDependencyBlocker = hasDependencyBlocker;
    }

    public boolean isHasRetentionBlocker() {
        return hasRetentionBlocker;
    }

    public void setHasRetentionBlocker(boolean hasRetentionBlocker) {
        this.hasRetentionBlocker = hasRetentionBlocker;
    }

    public String getBlockerReasons() {
        return blockerReasons;
    }

    public void setBlockerReasons(String blockerReasons) {
        this.blockerReasons = blockerReasons;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }
}

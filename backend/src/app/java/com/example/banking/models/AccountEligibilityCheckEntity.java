package com.example.banking.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_eligibility_checks")
public class AccountEligibilityCheckEntity {
    @Id
    @Column(name = "check_id", nullable = false, length = 36)
    private String checkId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "account_type", nullable = false, length = 16)
    private String accountType;

    @Column(name = "evaluated_at_utc", nullable = false)
    private Instant evaluatedAtUtc;

    @Column(name = "is_eligible", nullable = false)
    private boolean eligible;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    public AccountEligibilityCheckEntity() {
        this.checkId = UUID.randomUUID().toString();
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public Instant getEvaluatedAtUtc() {
        return evaluatedAtUtc;
    }

    public void setEvaluatedAtUtc(Instant evaluatedAtUtc) {
        this.evaluatedAtUtc = evaluatedAtUtc;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}

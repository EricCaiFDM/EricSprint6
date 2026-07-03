package com.example.banking.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "standing_orders")
public class StandingOrderEntity {
    @Id
    @Column(name = "standing_order_id", nullable = false, length = 36)
    private String standingOrderId;

    @Column(name = "source_account_id", nullable = false, length = 36)
    private String sourceAccountId;

    @Column(name = "destination_account_id", nullable = false, length = 36)
    private String destinationAccountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "cadence", nullable = false, length = 16)
    private StandingOrderCadence cadence;

    @Column(name = "schedule_config", columnDefinition = "TEXT")
    private String scheduleConfig;

    @Column(name = "effective_from_utc", nullable = false)
    private Instant effectiveFromUtc;

    @Column(name = "effective_to_utc")
    private Instant effectiveToUtc;

    @Column(name = "next_execution_at_utc")
    private Instant nextExecutionAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 16)
    private StandingOrderLifecycleState lifecycleState;

    @Column(name = "retry_policy_code", nullable = false, length = 32)
    private String retryPolicyCode;

    @Column(name = "created_by_user_id", nullable = false, length = 36)
    private String createdByUserId;

    @Column(name = "updated_at_utc", nullable = false)
    private Instant updatedAtUtc;

    @PrePersist
    void onCreate() {
        if (standingOrderId == null) {
            standingOrderId = UUID.randomUUID().toString();
        }
        if (updatedAtUtc == null) {
            updatedAtUtc = Instant.now();
        }
    }

    public String getStandingOrderId() {
        return standingOrderId;
    }

    public void setStandingOrderId(String standingOrderId) {
        this.standingOrderId = standingOrderId;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(String sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(String destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public StandingOrderCadence getCadence() {
        return cadence;
    }

    public void setCadence(StandingOrderCadence cadence) {
        this.cadence = cadence;
    }

    public String getScheduleConfig() {
        return scheduleConfig;
    }

    public void setScheduleConfig(String scheduleConfig) {
        this.scheduleConfig = scheduleConfig;
    }

    public Instant getEffectiveFromUtc() {
        return effectiveFromUtc;
    }

    public void setEffectiveFromUtc(Instant effectiveFromUtc) {
        this.effectiveFromUtc = effectiveFromUtc;
    }

    public Instant getEffectiveToUtc() {
        return effectiveToUtc;
    }

    public void setEffectiveToUtc(Instant effectiveToUtc) {
        this.effectiveToUtc = effectiveToUtc;
    }

    public Instant getNextExecutionAtUtc() {
        return nextExecutionAtUtc;
    }

    public void setNextExecutionAtUtc(Instant nextExecutionAtUtc) {
        this.nextExecutionAtUtc = nextExecutionAtUtc;
    }

    public StandingOrderLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(StandingOrderLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public String getRetryPolicyCode() {
        return retryPolicyCode;
    }

    public void setRetryPolicyCode(String retryPolicyCode) {
        this.retryPolicyCode = retryPolicyCode;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Instant getUpdatedAtUtc() {
        return updatedAtUtc;
    }

    public void setUpdatedAtUtc(Instant updatedAtUtc) {
        this.updatedAtUtc = updatedAtUtc;
    }
}

package com.example.banking.models;

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
@Table(name = "standing_order_execution_events")
public class StandingOrderExecutionEventEntity {
    @Id
    @Column(name = "execution_event_id", nullable = false, length = 36)
    private String executionEventId;

    @Column(name = "standing_order_id", nullable = false, length = 36)
    private String standingOrderId;

    @Column(name = "due_at_utc", nullable = false)
    private Instant dueAtUtc;

    @Column(name = "started_at_utc", nullable = false)
    private Instant startedAtUtc;

    @Column(name = "completed_at_utc")
    private Instant completedAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 48)
    private StandingOrderExecutionStatus status;

    @Column(name = "transfer_reference_id", length = 36)
    private String transferReferenceId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "next_retry_at_utc")
    private Instant nextRetryAtUtc;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    void onCreate() {
        if (executionEventId == null) {
            executionEventId = UUID.randomUUID().toString();
        }
        if (startedAtUtc == null) {
            startedAtUtc = Instant.now();
        }
    }

    public String getExecutionEventId() {
        return executionEventId;
    }

    public void setExecutionEventId(String executionEventId) {
        this.executionEventId = executionEventId;
    }

    public String getStandingOrderId() {
        return standingOrderId;
    }

    public void setStandingOrderId(String standingOrderId) {
        this.standingOrderId = standingOrderId;
    }

    public Instant getDueAtUtc() {
        return dueAtUtc;
    }

    public void setDueAtUtc(Instant dueAtUtc) {
        this.dueAtUtc = dueAtUtc;
    }

    public Instant getStartedAtUtc() {
        return startedAtUtc;
    }

    public void setStartedAtUtc(Instant startedAtUtc) {
        this.startedAtUtc = startedAtUtc;
    }

    public Instant getCompletedAtUtc() {
        return completedAtUtc;
    }

    public void setCompletedAtUtc(Instant completedAtUtc) {
        this.completedAtUtc = completedAtUtc;
    }

    public StandingOrderExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(StandingOrderExecutionStatus status) {
        this.status = status;
    }

    public String getTransferReferenceId() {
        return transferReferenceId;
    }

    public void setTransferReferenceId(String transferReferenceId) {
        this.transferReferenceId = transferReferenceId;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Instant getNextRetryAtUtc() {
        return nextRetryAtUtc;
    }

    public void setNextRetryAtUtc(Instant nextRetryAtUtc) {
        this.nextRetryAtUtc = nextRetryAtUtc;
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

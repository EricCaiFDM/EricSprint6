package com.example.banking.models.statement;

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
@Table(name = "statement_generation_events")
public class StatementGenerationEvent {
    @Id
    @Column(name = "generation_event_id", nullable = false, length = 36)
    private String generationEventId;

    @Column(name = "statement_id", length = 36)
    private String statementId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "period_year_month", nullable = false, length = 7)
    private String periodYearMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private StatementGenerationEventType eventType;

    @Column(name = "occurred_at_utc", nullable = false)
    private Instant occurredAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StatementEventStatus status;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    void onCreate() {
        if (generationEventId == null) {
            generationEventId = UUID.randomUUID().toString();
        }
        if (occurredAtUtc == null) {
            occurredAtUtc = Instant.now();
        }
    }

    public String getGenerationEventId() {
        return generationEventId;
    }

    public void setGenerationEventId(String generationEventId) {
        this.generationEventId = generationEventId;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPeriodYearMonth() {
        return periodYearMonth;
    }

    public void setPeriodYearMonth(String periodYearMonth) {
        this.periodYearMonth = periodYearMonth;
    }

    public StatementGenerationEventType getEventType() {
        return eventType;
    }

    public void setEventType(StatementGenerationEventType eventType) {
        this.eventType = eventType;
    }

    public Instant getOccurredAtUtc() {
        return occurredAtUtc;
    }

    public void setOccurredAtUtc(Instant occurredAtUtc) {
        this.occurredAtUtc = occurredAtUtc;
    }

    public StatementEventStatus getStatus() {
        return status;
    }

    public void setStatus(StatementEventStatus status) {
        this.status = status;
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

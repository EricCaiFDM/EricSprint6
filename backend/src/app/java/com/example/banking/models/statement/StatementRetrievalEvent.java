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
@Table(name = "statement_retrieval_events")
public class StatementRetrievalEvent {
    @Id
    @Column(name = "retrieval_event_id", nullable = false, length = 36)
    private String retrievalEventId;

    @Column(name = "statement_id", nullable = false, length = 36)
    private String statementId;

    @Column(name = "requester_user_id", nullable = false, length = 36)
    private String requesterUserId;

    @Column(name = "requester_role", nullable = false, length = 16)
    private String requesterRole;

    @Column(name = "occurred_at_utc", nullable = false)
    private Instant occurredAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 24)
    private StatementRetrievalOutcome outcome;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @PrePersist
    void onCreate() {
        if (retrievalEventId == null) {
            retrievalEventId = UUID.randomUUID().toString();
        }
        if (occurredAtUtc == null) {
            occurredAtUtc = Instant.now();
        }
    }

    public String getRetrievalEventId() {
        return retrievalEventId;
    }

    public void setRetrievalEventId(String retrievalEventId) {
        this.retrievalEventId = retrievalEventId;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public String getRequesterUserId() {
        return requesterUserId;
    }

    public void setRequesterUserId(String requesterUserId) {
        this.requesterUserId = requesterUserId;
    }

    public String getRequesterRole() {
        return requesterRole;
    }

    public void setRequesterRole(String requesterRole) {
        this.requesterRole = requesterRole;
    }

    public Instant getOccurredAtUtc() {
        return occurredAtUtc;
    }

    public void setOccurredAtUtc(Instant occurredAtUtc) {
        this.occurredAtUtc = occurredAtUtc;
    }

    public StatementRetrievalOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(StatementRetrievalOutcome outcome) {
        this.outcome = outcome;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }
}

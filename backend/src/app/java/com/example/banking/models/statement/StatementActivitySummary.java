package com.example.banking.models.statement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "statement_activity_summaries")
public class StatementActivitySummary {
    @Id
    @Column(name = "activity_summary_id", nullable = false, length = 36)
    private String activitySummaryId;

    @Column(name = "statement_id", nullable = false, length = 36, unique = true)
    private String statementId;

    @Column(name = "debit_total", nullable = false)
    private BigDecimal debitTotal;

    @Column(name = "credit_total", nullable = false)
    private BigDecimal creditTotal;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Column(name = "included_event_start_utc", nullable = false)
    private Instant includedEventStartUtc;

    @Column(name = "included_event_end_utc", nullable = false)
    private Instant includedEventEndUtc;

    @PrePersist
    void onCreate() {
        if (activitySummaryId == null) {
            activitySummaryId = UUID.randomUUID().toString();
        }
    }

    public String getActivitySummaryId() {
        return activitySummaryId;
    }

    public void setActivitySummaryId(String activitySummaryId) {
        this.activitySummaryId = activitySummaryId;
    }

    public String getStatementId() {
        return statementId;
    }

    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    public BigDecimal getDebitTotal() {
        return debitTotal;
    }

    public void setDebitTotal(BigDecimal debitTotal) {
        this.debitTotal = debitTotal;
    }

    public BigDecimal getCreditTotal() {
        return creditTotal;
    }

    public void setCreditTotal(BigDecimal creditTotal) {
        this.creditTotal = creditTotal;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Instant getIncludedEventStartUtc() {
        return includedEventStartUtc;
    }

    public void setIncludedEventStartUtc(Instant includedEventStartUtc) {
        this.includedEventStartUtc = includedEventStartUtc;
    }

    public Instant getIncludedEventEndUtc() {
        return includedEventEndUtc;
    }

    public void setIncludedEventEndUtc(Instant includedEventEndUtc) {
        this.includedEventEndUtc = includedEventEndUtc;
    }
}

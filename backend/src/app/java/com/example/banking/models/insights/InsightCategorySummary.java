package com.example.banking.models.insights;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "insight_category_summaries")
public class InsightCategorySummary {
    @Id
    @Column(name = "summary_id", nullable = false, length = 36)
    private String summaryId;

    @Column(name = "insight_id", nullable = false, length = 36)
    private String insightId;

    @Column(name = "category_code", nullable = false, length = 64)
    private String categoryCode;

    @Column(name = "category_label", nullable = false, length = 96)
    private String categoryLabel;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(name = "period_share_percent", nullable = false)
    private BigDecimal periodSharePercent;

    @PrePersist
    void onCreate() {
        if (summaryId == null) {
            summaryId = UUID.randomUUID().toString();
        }
    }

    public String getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(String summaryId) {
        this.summaryId = summaryId;
    }

    public String getInsightId() {
        return insightId;
    }

    public void setInsightId(String insightId) {
        this.insightId = insightId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public void setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getPeriodSharePercent() {
        return periodSharePercent;
    }

    public void setPeriodSharePercent(BigDecimal periodSharePercent) {
        this.periodSharePercent = periodSharePercent;
    }
}

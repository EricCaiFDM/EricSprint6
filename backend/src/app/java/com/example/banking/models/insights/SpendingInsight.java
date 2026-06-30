package com.example.banking.models.insights;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "spending_insights")
public class SpendingInsight {
    @Id
    @Column(name = "insight_id", nullable = false, length = 36)
    private String insightId;

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "taxonomy_version", nullable = false, length = 32)
    private String taxonomyVersion;

    @Column(name = "generated_at_utc", nullable = false)
    private Instant generatedAtUtc;

    @Column(name = "status", nullable = false, length = 24)
    private String status;

    @Column(name = "total_spend_amount", nullable = false)
    private BigDecimal totalSpendAmount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "trend_direction", nullable = false, length = 24)
    private String trendDirection;

    @Column(name = "trend_delta_percent")
    private BigDecimal trendDeltaPercent;

    @PrePersist
    void onCreate() {
        if (insightId == null) {
            insightId = UUID.randomUUID().toString();
        }
        if (generatedAtUtc == null) {
            generatedAtUtc = Instant.now();
        }
    }

    public String getInsightId() {
        return insightId;
    }

    public void setInsightId(String insightId) {
        this.insightId = insightId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTaxonomyVersion() {
        return taxonomyVersion;
    }

    public void setTaxonomyVersion(String taxonomyVersion) {
        this.taxonomyVersion = taxonomyVersion;
    }

    public Instant getGeneratedAtUtc() {
        return generatedAtUtc;
    }

    public void setGeneratedAtUtc(Instant generatedAtUtc) {
        this.generatedAtUtc = generatedAtUtc;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getTotalSpendAmount() {
        return totalSpendAmount;
    }

    public void setTotalSpendAmount(BigDecimal totalSpendAmount) {
        this.totalSpendAmount = totalSpendAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTrendDirection() {
        return trendDirection;
    }

    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
    }

    public BigDecimal getTrendDeltaPercent() {
        return trendDeltaPercent;
    }

    public void setTrendDeltaPercent(BigDecimal trendDeltaPercent) {
        this.trendDeltaPercent = trendDeltaPercent;
    }
}

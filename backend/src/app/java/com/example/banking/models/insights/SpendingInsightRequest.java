package com.example.banking.models.insights;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "spending_insight_requests")
public class SpendingInsightRequest {
    @Id
    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    @Column(name = "scope_type", nullable = false, length = 16)
    private String scopeType;

    @Column(name = "scope_id", nullable = false, length = 36)
    private String scopeId;

    @Column(name = "period_start_utc", nullable = false)
    private Instant periodStartUtc;

    @Column(name = "period_end_utc", nullable = false)
    private Instant periodEndUtc;

    @Column(name = "category_filters", columnDefinition = "TEXT")
    private String categoryFilters;

    @Column(name = "requested_by_user_id", nullable = false, length = 36)
    private String requestedByUserId;

    @Column(name = "requested_at_utc", nullable = false)
    private Instant requestedAtUtc;

    @PrePersist
    void onCreate() {
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        if (requestedAtUtc == null) {
            requestedAtUtc = Instant.now();
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public Instant getPeriodStartUtc() {
        return periodStartUtc;
    }

    public void setPeriodStartUtc(Instant periodStartUtc) {
        this.periodStartUtc = periodStartUtc;
    }

    public Instant getPeriodEndUtc() {
        return periodEndUtc;
    }

    public void setPeriodEndUtc(Instant periodEndUtc) {
        this.periodEndUtc = periodEndUtc;
    }

    public String getCategoryFilters() {
        return categoryFilters;
    }

    public void setCategoryFilters(String categoryFilters) {
        this.categoryFilters = categoryFilters;
    }

    public String getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(String requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public Instant getRequestedAtUtc() {
        return requestedAtUtc;
    }

    public void setRequestedAtUtc(Instant requestedAtUtc) {
        this.requestedAtUtc = requestedAtUtc;
    }
}

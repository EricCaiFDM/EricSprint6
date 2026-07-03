package com.example.banking.api.insights;

import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for spending insight query parameters.")
public class SpendingInsightQuery {
    @Pattern(regexp = "^(?i)(ACCOUNT|CUSTOMER)?$", message = "scopeType must be ACCOUNT or CUSTOMER")
    private String scopeType;

    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "scopeId must be a UUID")
    private String scopeId;

    private String periodStartUtc;
    private String periodEndUtc;
    private String categoryFilters;

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

    public String getPeriodStartUtc() {
        return periodStartUtc;
    }

    public void setPeriodStartUtc(String periodStartUtc) {
        this.periodStartUtc = periodStartUtc;
    }

    public String getPeriodEndUtc() {
        return periodEndUtc;
    }

    public void setPeriodEndUtc(String periodEndUtc) {
        this.periodEndUtc = periodEndUtc;
    }

    public String getCategoryFilters() {
        return categoryFilters;
    }

    public void setCategoryFilters(String categoryFilters) {
        this.categoryFilters = categoryFilters;
    }
}

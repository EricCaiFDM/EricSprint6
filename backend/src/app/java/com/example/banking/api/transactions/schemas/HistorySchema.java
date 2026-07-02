package com.example.banking.api.transactions.schemas;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for history schema.")
public class HistorySchema {
    @NotBlank(message = "scopeType is required")
    private String scopeType;

    @NotBlank(message = "scopeId is required")
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "scopeId must be a UUID")
    private String scopeId;

    private String startDateUtc;
    private String endDateUtc;
    private String transactionType;

    @Min(value = 1, message = "page must be greater than or equal to 1")
    private Integer page = 1;

    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = 100, message = "pageSize must be less than or equal to 100")
    private Integer pageSize = 20;

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

    public String getStartDateUtc() {
        return startDateUtc;
    }

    public void setStartDateUtc(String startDateUtc) {
        this.startDateUtc = startDateUtc;
    }

    public String getEndDateUtc() {
        return endDateUtc;
    }

    public void setEndDateUtc(String endDateUtc) {
        this.endDateUtc = endDateUtc;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}

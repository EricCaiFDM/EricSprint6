package com.example.banking.models.statement;

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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "monthly_statements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_monthly_statements_account_period_version",
                        columnNames = { "account_id", "period_year_month", "artifact_version" })
        })
public class MonthlyStatement {
    @Id
    @Column(name = "statement_id", nullable = false, length = 36)
    private String statementId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "period_year_month", nullable = false, length = 7)
    private String periodYearMonth;

    @Column(name = "period_start_utc", nullable = false)
    private Instant periodStartUtc;

    @Column(name = "period_end_utc", nullable = false)
    private Instant periodEndUtc;

    @Column(name = "opening_balance", nullable = false)
    private BigDecimal openingBalance;

    @Column(name = "closing_balance", nullable = false)
    private BigDecimal closingBalance;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "artifact_version", nullable = false)
    private Integer artifactVersion;

    @Column(name = "artifact_uri", nullable = false, length = 512)
    private String artifactUri;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_mode", nullable = false, length = 16)
    private StatementGenerationMode generationMode;

    @Column(name = "generated_at_utc", nullable = false)
    private Instant generatedAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private MonthlyStatementStatus status;

    @PrePersist
    void onCreate() {
        if (statementId == null) {
            statementId = UUID.randomUUID().toString();
        }
        if (generatedAtUtc == null) {
            generatedAtUtc = Instant.now();
        }
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

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Integer getArtifactVersion() {
        return artifactVersion;
    }

    public void setArtifactVersion(Integer artifactVersion) {
        this.artifactVersion = artifactVersion;
    }

    public String getArtifactUri() {
        return artifactUri;
    }

    public void setArtifactUri(String artifactUri) {
        this.artifactUri = artifactUri;
    }

    public StatementGenerationMode getGenerationMode() {
        return generationMode;
    }

    public void setGenerationMode(StatementGenerationMode generationMode) {
        this.generationMode = generationMode;
    }

    public Instant getGeneratedAtUtc() {
        return generatedAtUtc;
    }

    public void setGeneratedAtUtc(Instant generatedAtUtc) {
        this.generatedAtUtc = generatedAtUtc;
    }

    public MonthlyStatementStatus getStatus() {
        return status;
    }

    public void setStatus(MonthlyStatementStatus status) {
        this.status = status;
    }
}

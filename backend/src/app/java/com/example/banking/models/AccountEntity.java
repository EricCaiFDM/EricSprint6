package com.example.banking.models;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id
    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "account_number", nullable = false, length = 24)
    private String accountNumber;

    @Column(name = "account_type", nullable = false, length = 16)
    private String accountType;

    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    @Column(name = "checking_number")
    private Integer checkingNumber;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "opened_at_utc", nullable = false)
    private Instant openedAtUtc;

    @Column(name = "closed_at_utc")
    private Instant closedAtUtc;

    @Column(name = "created_by_user_id", nullable = false, length = 36)
    private String createdByUserId;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Column(name = "updated_at_utc", nullable = false)
    private Instant updatedAtUtc;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Integer getCheckingNumber() {
        return checkingNumber;
    }

    public void setCheckingNumber(Integer checkingNumber) {
        this.checkingNumber = checkingNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Instant getOpenedAtUtc() {
        return openedAtUtc;
    }

    public void setOpenedAtUtc(Instant openedAtUtc) {
        this.openedAtUtc = openedAtUtc;
    }

    public Instant getClosedAtUtc() {
        return closedAtUtc;
    }

    public void setClosedAtUtc(Instant closedAtUtc) {
        this.closedAtUtc = closedAtUtc;
    }

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(String createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Instant getUpdatedAtUtc() {
        return updatedAtUtc;
    }

    public void setUpdatedAtUtc(Instant updatedAtUtc) {
        this.updatedAtUtc = updatedAtUtc;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}

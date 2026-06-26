package com.example.banking.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "transfer_links")
public class TransferLinkEntity {
    @Id
    @Column(name = "transfer_id", nullable = false, length = 36)
    private String transferId;

    @Column(name = "debit_transaction_id", nullable = false, length = 36, unique = true)
    private String debitTransactionId;

    @Column(name = "credit_transaction_id", nullable = false, length = 36, unique = true)
    private String creditTransactionId;

    @Column(name = "source_account_id", nullable = false, length = 36)
    private String sourceAccountId;

    @Column(name = "destination_account_id", nullable = false, length = 36)
    private String destinationAccountId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @PrePersist
    void onCreate() {
        if (transferId == null) {
            transferId = UUID.randomUUID().toString();
        }
        if (createdAtUtc == null) {
            createdAtUtc = Instant.now();
        }
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public String getDebitTransactionId() {
        return debitTransactionId;
    }

    public void setDebitTransactionId(String debitTransactionId) {
        this.debitTransactionId = debitTransactionId;
    }

    public String getCreditTransactionId() {
        return creditTransactionId;
    }

    public void setCreditTransactionId(String creditTransactionId) {
        this.creditTransactionId = creditTransactionId;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(String sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(String destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Instant getCreatedAtUtc() {
        return createdAtUtc;
    }

    public void setCreatedAtUtc(Instant createdAtUtc) {
        this.createdAtUtc = createdAtUtc;
    }
}

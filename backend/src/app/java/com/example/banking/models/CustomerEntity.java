package com.example.banking.models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "external_customer_key", nullable = false, length = 120)
    private String externalCustomerKey;

    @Column(name = "legal_name", nullable = false, length = 160)
    private String legalName;

    @Column(name = "primary_email", nullable = false, length = 255)
    private String primaryEmail;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_at_utc", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc", nullable = false)
    private Instant updatedAtUtc;

    @Column(name = "created_by_user_id", nullable = false, length = 36)
    private String createdByUserId;

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private String ownerUserId;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getExternalCustomerKey() {
        return externalCustomerKey;
    }

    public void setExternalCustomerKey(String externalCustomerKey) {
        this.externalCustomerKey = externalCustomerKey;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public void setPrimaryEmail(String primaryEmail) {
        this.primaryEmail = primaryEmail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAtUtc() {
        return createdAtUtc;
    }

    public void setCreatedAtUtc(Instant createdAtUtc) {
        this.createdAtUtc = createdAtUtc;
    }

    public Instant getUpdatedAtUtc() {
        return updatedAtUtc;
    }

    public void setUpdatedAtUtc(Instant updatedAtUtc) {
        this.updatedAtUtc = updatedAtUtc;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}

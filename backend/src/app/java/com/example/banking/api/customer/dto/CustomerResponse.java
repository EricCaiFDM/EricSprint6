package com.example.banking.api.customer.dto;

public record CustomerResponse(
        String customerId,
        String externalCustomerKey,
        String legalName,
        String primaryEmail,
        String phoneNumber,
        String status,
        String createdAtUtc,
        String updatedAtUtc,
        String createdByUserId,
        String ownerUserId) {
}

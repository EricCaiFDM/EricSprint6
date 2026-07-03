package com.example.banking.api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for customer response payload.")
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

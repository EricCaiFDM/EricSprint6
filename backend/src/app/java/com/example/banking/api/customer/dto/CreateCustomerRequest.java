package com.example.banking.api.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank(message = "externalCustomerKey is required")
        @Size(max = 120, message = "externalCustomerKey must not exceed 120 characters")
        String externalCustomerKey,

        @NotBlank(message = "legalName is required")
        @Size(max = 160, message = "legalName must not exceed 160 characters")
        String legalName,

        @NotBlank(message = "primaryEmail is required")
        @Email(message = "primaryEmail must be valid")
        @Size(max = 255, message = "primaryEmail must not exceed 255 characters")
        String primaryEmail,

        @Pattern(regexp = "^[0-9+()\\-\\s]{7,32}$", message = "phoneNumber must be a valid phone number")
        String phoneNumber) {
}

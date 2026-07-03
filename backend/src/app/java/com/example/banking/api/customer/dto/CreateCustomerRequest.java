package com.example.banking.api.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for create customer request payload.")
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

        @Size(min = 7, max = 32, message = "phoneNumber must be between 7 and 32 characters")
        @Pattern(regexp = "^[0-9+()\\-\\s]+$", message = "phoneNumber must contain only digits, spaces, +, -, and parentheses")
        String phoneNumber,

        @Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
        String password) {
}

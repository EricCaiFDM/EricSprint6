package com.example.banking.api.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @Size(max = 160, message = "legalName must not exceed 160 characters")
        String legalName,

        @Email(message = "primaryEmail must be valid")
        @Size(max = 255, message = "primaryEmail must not exceed 255 characters")
        String primaryEmail,

        @Size(min = 7, max = 32, message = "phoneNumber must be between 7 and 32 characters")
        @Pattern(regexp = "^[0-9+()\\-\\s]+$", message = "phoneNumber must contain only digits, spaces, +, -, and parentheses")
        String phoneNumber,

        @Pattern(regexp = "^(ACTIVE|SUSPENDED|CLOSED)$", message = "status must be ACTIVE, SUSPENDED, or CLOSED")
        String status) {
}

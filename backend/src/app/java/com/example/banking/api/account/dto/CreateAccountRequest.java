package com.example.banking.api.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for create account request payload.")
public record CreateAccountRequest(
        @NotBlank(message = "customerId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "customerId must be a UUID")
        String customerId,

        @NotBlank(message = "accountType is required")
        String accountType,

        @NotBlank(message = "currencyCode is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be a 3-letter ISO code")
        String currencyCode,

        @Size(max = 64, message = "nickname must not exceed 64 characters")
        String nickname,

        @DecimalMin(value = "0.00", message = "interestRate must be greater than or equal to 0")
        BigDecimal interestRate) {
}

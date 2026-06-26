package com.example.banking.api.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
        String nickname) {
}

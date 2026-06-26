package com.example.banking.api.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GetAccountRequest(
        @NotBlank(message = "accountId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "accountId must be a UUID")
        String accountId) {
}

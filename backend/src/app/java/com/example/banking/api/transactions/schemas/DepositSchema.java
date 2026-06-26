package com.example.banking.api.transactions.schemas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DepositSchema(
        @NotBlank(message = "accountId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "accountId must be a UUID")
        String accountId,

        @NotBlank(message = "amount is required")
        String amount) {
}

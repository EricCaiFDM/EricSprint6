package com.example.banking.api.transactions.schemas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for withdrawal schema.")
public record WithdrawalSchema(
        @NotBlank(message = "accountId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "accountId must be a UUID")
        String accountId,

        @NotBlank(message = "amount is required")
        String amount) {
}

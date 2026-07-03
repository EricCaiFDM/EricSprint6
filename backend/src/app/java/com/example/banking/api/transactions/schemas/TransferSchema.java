package com.example.banking.api.transactions.schemas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for transfer schema.")
public record TransferSchema(
        @NotBlank(message = "sourceAccountId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "sourceAccountId must be a UUID")
        String sourceAccountId,

        @NotBlank(message = "destinationAccountId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "destinationAccountId must be a UUID")
        String destinationAccountId,

        @NotBlank(message = "amount is required")
        String amount) {
}

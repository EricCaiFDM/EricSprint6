package com.example.banking.api.statements.schemas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for generate statement request schema payload.")
public record GenerateStatementRequestSchema(
        @NotBlank(message = "accountId is required")
        @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "accountId must be a UUID")
        String accountId,

        @NotBlank(message = "periodYearMonth is required")
        @Pattern(regexp = "^[0-9]{4}-(0[1-9]|1[0-2])$", message = "periodYearMonth must follow YYYY-MM")
        String periodYearMonth,

        @NotBlank(message = "generationMode is required")
        @Pattern(regexp = "^(STANDARD|CORRECTION)$", message = "generationMode must be STANDARD or CORRECTION")
        String generationMode) {
}

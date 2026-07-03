package com.example.banking.api.statements.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for statement response schema payload.")
public record StatementResponseSchema(
        String statementId,
        String accountId,
        String periodYearMonth,
        int artifactVersion,
        String openingBalance,
        String closingBalance,
        String currencyCode,
        String status,
        String artifactUri,
        String generatedAtUtc) {
}

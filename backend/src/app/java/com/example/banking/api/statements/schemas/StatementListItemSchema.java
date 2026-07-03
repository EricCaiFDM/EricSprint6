package com.example.banking.api.statements.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for statement list item schema.")
public record StatementListItemSchema(
        String statementId,
        String accountId,
        String periodYearMonth,
        int artifactVersion,
        String status,
        String generatedAtUtc) {
}

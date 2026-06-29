package com.example.banking.api.statements.schemas;

public record StatementListItemSchema(
        String statementId,
        String accountId,
        String periodYearMonth,
        int artifactVersion,
        String status,
        String generatedAtUtc) {
}

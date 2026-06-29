package com.example.banking.api.statements.schemas;

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

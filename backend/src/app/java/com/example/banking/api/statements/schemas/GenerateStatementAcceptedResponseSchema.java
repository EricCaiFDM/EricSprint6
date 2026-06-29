package com.example.banking.api.statements.schemas;

public record GenerateStatementAcceptedResponseSchema(
        String statementId,
        String generationStatus) {
}

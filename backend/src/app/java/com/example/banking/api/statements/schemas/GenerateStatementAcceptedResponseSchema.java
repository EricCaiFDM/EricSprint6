package com.example.banking.api.statements.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for generate statement accepted response schema payload.")
public record GenerateStatementAcceptedResponseSchema(
        String statementId,
        String generationStatus) {
}

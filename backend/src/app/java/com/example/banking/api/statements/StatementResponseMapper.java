package com.example.banking.api.statements;

import org.springframework.stereotype.Component;

import com.example.banking.api.statements.schemas.StatementListItemSchema;
import com.example.banking.api.statements.schemas.StatementResponseSchema;
import com.example.banking.models.statement.MonthlyStatement;

@Component
public class StatementResponseMapper {
    public StatementResponseSchema toResponse(MonthlyStatement statement) {
        return new StatementResponseSchema(
                statement.getStatementId(),
                statement.getAccountId(),
                statement.getPeriodYearMonth(),
                statement.getArtifactVersion() == null ? 1 : statement.getArtifactVersion(),
                statement.getOpeningBalance().toPlainString(),
                statement.getClosingBalance().toPlainString(),
                statement.getCurrencyCode(),
                statement.getStatus().name(),
                statement.getArtifactUri(),
                statement.getGeneratedAtUtc() == null ? null : statement.getGeneratedAtUtc().toString());
    }

    public StatementListItemSchema toListItem(MonthlyStatement statement) {
        return new StatementListItemSchema(
                statement.getStatementId(),
                statement.getAccountId(),
                statement.getPeriodYearMonth(),
                statement.getArtifactVersion() == null ? 1 : statement.getArtifactVersion(),
                statement.getStatus().name(),
                statement.getGeneratedAtUtc() == null ? null : statement.getGeneratedAtUtc().toString());
    }
}

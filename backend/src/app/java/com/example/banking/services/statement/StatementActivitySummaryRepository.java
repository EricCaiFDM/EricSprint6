package com.example.banking.services.statement;

import java.util.Optional;

import com.example.banking.models.statement.StatementActivitySummary;

public interface StatementActivitySummaryRepository {
    StatementActivitySummary save(StatementActivitySummary summary);

    Optional<StatementActivitySummary> findByStatementId(String statementId);
}

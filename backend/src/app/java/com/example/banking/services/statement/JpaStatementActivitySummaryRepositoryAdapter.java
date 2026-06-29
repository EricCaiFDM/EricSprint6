package com.example.banking.services.statement;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.banking.lib.StatementActivitySummaryJpaRepository;
import com.example.banking.models.statement.StatementActivitySummary;

@Repository
public class JpaStatementActivitySummaryRepositoryAdapter implements StatementActivitySummaryRepository {
    private final StatementActivitySummaryJpaRepository statementActivitySummaryJpaRepository;

    public JpaStatementActivitySummaryRepositoryAdapter(
            StatementActivitySummaryJpaRepository statementActivitySummaryJpaRepository) {
        this.statementActivitySummaryJpaRepository = statementActivitySummaryJpaRepository;
    }

    @Override
    public StatementActivitySummary save(StatementActivitySummary summary) {
        return statementActivitySummaryJpaRepository.save(summary);
    }

    @Override
    public Optional<StatementActivitySummary> findByStatementId(String statementId) {
        return statementActivitySummaryJpaRepository.findByStatementId(statementId);
    }
}

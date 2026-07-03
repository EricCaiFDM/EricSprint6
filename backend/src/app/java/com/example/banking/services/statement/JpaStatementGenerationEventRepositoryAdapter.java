package com.example.banking.services.statement;

import org.springframework.stereotype.Repository;

import com.example.banking.lib.StatementGenerationEventJpaRepository;
import com.example.banking.models.statement.StatementGenerationEvent;

@Repository
public class JpaStatementGenerationEventRepositoryAdapter implements StatementGenerationEventRepository {
    private final StatementGenerationEventJpaRepository statementGenerationEventJpaRepository;

    public JpaStatementGenerationEventRepositoryAdapter(
            StatementGenerationEventJpaRepository statementGenerationEventJpaRepository) {
        this.statementGenerationEventJpaRepository = statementGenerationEventJpaRepository;
    }

    @Override
    public StatementGenerationEvent save(StatementGenerationEvent event) {
        return statementGenerationEventJpaRepository.save(event);
    }
}

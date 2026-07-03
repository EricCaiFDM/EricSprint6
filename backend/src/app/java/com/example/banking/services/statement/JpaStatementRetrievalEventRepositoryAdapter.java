package com.example.banking.services.statement;

import org.springframework.stereotype.Repository;

import com.example.banking.lib.StatementRetrievalEventJpaRepository;
import com.example.banking.models.statement.StatementRetrievalEvent;

@Repository
public class JpaStatementRetrievalEventRepositoryAdapter implements StatementRetrievalEventRepository {
    private final StatementRetrievalEventJpaRepository statementRetrievalEventJpaRepository;

    public JpaStatementRetrievalEventRepositoryAdapter(
            StatementRetrievalEventJpaRepository statementRetrievalEventJpaRepository) {
        this.statementRetrievalEventJpaRepository = statementRetrievalEventJpaRepository;
    }

    @Override
    public StatementRetrievalEvent save(StatementRetrievalEvent event) {
        return statementRetrievalEventJpaRepository.save(event);
    }
}

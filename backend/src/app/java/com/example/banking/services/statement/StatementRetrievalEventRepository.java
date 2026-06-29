package com.example.banking.services.statement;

import com.example.banking.models.statement.StatementRetrievalEvent;

public interface StatementRetrievalEventRepository {
    StatementRetrievalEvent save(StatementRetrievalEvent event);
}

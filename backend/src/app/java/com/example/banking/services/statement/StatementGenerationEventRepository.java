package com.example.banking.services.statement;

import com.example.banking.models.statement.StatementGenerationEvent;

public interface StatementGenerationEventRepository {
    StatementGenerationEvent save(StatementGenerationEvent event);
}

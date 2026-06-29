package com.example.banking.services.statement;

import java.util.Optional;

import org.springframework.data.domain.Page;

import com.example.banking.models.statement.MonthlyStatement;

public interface MonthlyStatementRepository {
    MonthlyStatement save(MonthlyStatement statement);

    Optional<MonthlyStatement> findById(String statementId);

    Optional<MonthlyStatement> findLatestByAccountAndPeriod(String accountId, String periodYearMonth);

    Page<MonthlyStatement> listByScope(
            String actorUserId,
            String role,
            String accountId,
            String periodYearMonth,
            int page,
            int pageSize);
}

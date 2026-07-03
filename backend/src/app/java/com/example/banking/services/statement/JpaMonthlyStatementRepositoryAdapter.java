package com.example.banking.services.statement;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.example.banking.lib.MonthlyStatementJpaRepository;
import com.example.banking.models.statement.MonthlyStatement;

@Repository
public class JpaMonthlyStatementRepositoryAdapter implements MonthlyStatementRepository {
    private final MonthlyStatementJpaRepository monthlyStatementJpaRepository;

    public JpaMonthlyStatementRepositoryAdapter(MonthlyStatementJpaRepository monthlyStatementJpaRepository) {
        this.monthlyStatementJpaRepository = monthlyStatementJpaRepository;
    }

    @Override
    public MonthlyStatement save(MonthlyStatement statement) {
        return monthlyStatementJpaRepository.save(statement);
    }

    @Override
    public Optional<MonthlyStatement> findById(String statementId) {
        return monthlyStatementJpaRepository.findByStatementId(statementId);
    }

    @Override
    public Optional<MonthlyStatement> findLatestByAccountAndPeriod(String accountId, String periodYearMonth) {
        return monthlyStatementJpaRepository
                .findByAccountIdAndPeriodYearMonthOrderByArtifactVersionDesc(accountId, periodYearMonth)
                .stream()
                .findFirst();
    }

    @Override
    public Page<MonthlyStatement> listByScope(
            String actorUserId,
            String role,
            String accountId,
            String periodYearMonth,
            int page,
            int pageSize) {
        int normalizedPage = Math.max(page - 1, 0);
        int normalizedPageSize = Math.max(pageSize, 1);
        return monthlyStatementJpaRepository.listByScope(
                actorUserId,
                role,
                accountId,
                periodYearMonth,
                PageRequest.of(normalizedPage, normalizedPageSize, Sort.by(Sort.Direction.DESC, "generatedAtUtc")));
    }
}

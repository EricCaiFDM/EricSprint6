package com.example.banking.services.statement;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.banking.lib.config.StatementModuleConfig;
import com.example.banking.lib.errors.StatementErrors;
import com.example.banking.lib.security.StatementAccessGuard;
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.models.statement.StatementAccessPolicy;

@Service
public class StatementQueryService {
    private final MonthlyStatementRepository monthlyStatementRepository;
    private final StatementAccessGuard statementAccessGuard;
    private final StatementAccessPolicyService statementAccessPolicyService;
    private final StatementModuleConfig statementModuleConfig;

    public StatementQueryService(
            MonthlyStatementRepository monthlyStatementRepository,
            StatementAccessGuard statementAccessGuard,
            StatementAccessPolicyService statementAccessPolicyService,
            StatementModuleConfig statementModuleConfig) {
        this.monthlyStatementRepository = monthlyStatementRepository;
        this.statementAccessGuard = statementAccessGuard;
        this.statementAccessPolicyService = statementAccessPolicyService;
        this.statementModuleConfig = statementModuleConfig;
    }

    public Page<MonthlyStatement> list(
            String actorUserId,
            String role,
            String accountId,
            String periodYearMonth,
            int page,
            int pageSize) {
        StatementAccessPolicy policy = statementAccessPolicyService.resolve(actorUserId, role);
        statementAccessGuard.enforceRetrievalAccess(policy.role());

        String normalizedAccountId = normalizeUuid(accountId, "accountId");
        statementAccessGuard.requireAccountScope(normalizedAccountId, policy.role(), policy.userId(), "read");

        String normalizedPeriodYearMonth = normalizePeriod(periodYearMonth);
        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, statementModuleConfig.getMaxPageSize()));

        return monthlyStatementRepository.listByScope(
                policy.userId(),
                normalizeRole(policy.role()),
                normalizedAccountId,
                normalizedPeriodYearMonth,
                normalizedPage,
                normalizedPageSize);
    }

    private String normalizeUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw StatementErrors.validation(field + " is required", field);
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw StatementErrors.validation(field + " must be a UUID", field);
        }
    }

    private String normalizePeriod(String periodYearMonth) {
        if (periodYearMonth == null || periodYearMonth.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(periodYearMonth.trim()).toString();
        } catch (DateTimeParseException exception) {
            throw StatementErrors.validation("periodYearMonth must follow YYYY-MM", "periodYearMonth");
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}

package com.example.banking.services.statement;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.config.StatementModuleConfig;
import com.example.banking.lib.errors.StatementErrors;
import com.example.banking.lib.security.StatementAccessGuard;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.statement.MonthlyStatement;
import com.example.banking.models.statement.MonthlyStatementStatus;
import com.example.banking.models.statement.StatementActivitySummary;
import com.example.banking.models.statement.StatementEventStatus;
import com.example.banking.models.statement.StatementGenerationEvent;
import com.example.banking.models.statement.StatementGenerationEventType;
import com.example.banking.models.statement.StatementGenerationMode;

@Service
public class StatementGenerationService {
    private final StatementAccessGuard statementAccessGuard;
    private final StatementComputationService statementComputationService;
    private final MonthlyStatementRepository monthlyStatementRepository;
    private final StatementActivitySummaryRepository statementActivitySummaryRepository;
    private final StatementGenerationEventRepository statementGenerationEventRepository;
    private final StatementModuleConfig statementModuleConfig;

    public StatementGenerationService(
            StatementAccessGuard statementAccessGuard,
            StatementComputationService statementComputationService,
            MonthlyStatementRepository monthlyStatementRepository,
            StatementActivitySummaryRepository statementActivitySummaryRepository,
            StatementGenerationEventRepository statementGenerationEventRepository,
            StatementModuleConfig statementModuleConfig) {
        this.statementAccessGuard = statementAccessGuard;
        this.statementComputationService = statementComputationService;
        this.monthlyStatementRepository = monthlyStatementRepository;
        this.statementActivitySummaryRepository = statementActivitySummaryRepository;
        this.statementGenerationEventRepository = statementGenerationEventRepository;
        this.statementModuleConfig = statementModuleConfig;
    }

    @Transactional
    public MonthlyStatement generate(
            String accountId,
            String periodYearMonth,
            String generationMode,
            String actorUserId,
            String role) {
        statementAccessGuard.enforceGenerationAccess(role);
        String normalizedActor = normalizeActor(actorUserId);
        String normalizedRole = normalizeRole(role);
        String normalizedAccountId = normalizeUuid(accountId, "accountId");
        AccountEntity account = statementAccessGuard.requireAccountScope(
                normalizedAccountId,
                normalizedRole,
                normalizedActor,
                "generate");

        YearMonth period = normalizePeriod(periodYearMonth);
        StatementGenerationMode mode = normalizeMode(generationMode);

        recordGenerationEvent(
                null,
                normalizedAccountId,
                period.toString(),
                StatementGenerationEventType.GENERATION_STARTED,
                StatementEventStatus.SUCCESS,
                null,
                "{}");

        try {
            MonthlyStatement latest = monthlyStatementRepository
                    .findLatestByAccountAndPeriod(normalizedAccountId, period.toString())
                    .orElse(null);

            if (mode == StatementGenerationMode.STANDARD && latest != null) {
                throw StatementErrors.conflict(
                        "Standard statement already exists for the provided period",
                        "periodYearMonth");
            }

            if (mode == StatementGenerationMode.CORRECTION && latest == null) {
                throw StatementErrors.validation(
                        "Correction generation requires an existing statement for the period",
                        "generationMode");
            }

            int artifactVersion = latest == null ? 1 : latest.getArtifactVersion() + 1;
            Instant periodStartUtc = period.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant periodEndUtc = period.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            StatementComputationService.ComputationResult computation = statementComputationService.compute(
                    normalizedAccountId,
                    periodStartUtc,
                    periodEndUtc);

            String statementId = UUID.randomUUID().toString();
            MonthlyStatement statement = new MonthlyStatement();
            statement.setStatementId(statementId);
            statement.setAccountId(normalizedAccountId);
            statement.setPeriodYearMonth(period.toString());
            statement.setPeriodStartUtc(periodStartUtc);
            statement.setPeriodEndUtc(periodEndUtc);
            statement.setOpeningBalance(computation.openingBalance());
            statement.setClosingBalance(computation.closingBalance());
            statement.setCurrencyCode(account.getCurrencyCode());
            statement.setArtifactVersion(artifactVersion);
            statement.setArtifactUri(buildArtifactUri(statementId, artifactVersion));
            statement.setGenerationMode(mode);
            statement.setStatus(artifactVersion > 1
                    ? MonthlyStatementStatus.CORRECTED
                    : MonthlyStatementStatus.GENERATED);

            MonthlyStatement savedStatement = monthlyStatementRepository.save(statement);

            StatementActivitySummary summary = new StatementActivitySummary();
            summary.setStatementId(savedStatement.getStatementId());
            summary.setDebitTotal(computation.debitTotal());
            summary.setCreditTotal(computation.creditTotal());
            summary.setTransactionCount(computation.transactionCount());
            summary.setIncludedEventStartUtc(periodStartUtc);
            summary.setIncludedEventEndUtc(periodEndUtc);
            statementActivitySummaryRepository.save(summary);

            StatementGenerationEventType successEventType = artifactVersion > 1
                    ? StatementGenerationEventType.CORRECTION_GENERATED
                    : StatementGenerationEventType.GENERATION_SUCCEEDED;

            recordGenerationEvent(
                    savedStatement.getStatementId(),
                    normalizedAccountId,
                    period.toString(),
                    successEventType,
                    StatementEventStatus.SUCCESS,
                    null,
                    "{}");

            return savedStatement;
        } catch (ApiErrorException exception) {
            recordGenerationEvent(
                    null,
                    normalizedAccountId,
                    period.toString(),
                    StatementGenerationEventType.GENERATION_FAILED,
                    StatementEventStatus.FAILURE,
                    exception.getCode(),
                    "{\"message\":\"" + sanitize(exception.getMessage()) + "\"}");
            throw exception;
        } catch (Exception exception) {
            recordGenerationEvent(
                    null,
                    normalizedAccountId,
                    period.toString(),
                    StatementGenerationEventType.GENERATION_FAILED,
                    StatementEventStatus.FAILURE,
                    "STATEMENT_DEPENDENCY_FAILURE",
                    "{\"message\":\"" + sanitize(exception.getMessage()) + "\"}");
            throw StatementErrors.dependencyFailure("Statement generation failed due to dependency outage");
        }
    }

    @Transactional
    public void generateForScheduler(String accountId, String periodYearMonth) {
        try {
            generate(accountId, periodYearMonth, StatementGenerationMode.STANDARD.name(), "system-scheduler", "ADMIN");
        } catch (ApiErrorException exception) {
            if (!"STATEMENT_CONFLICT".equals(exception.getCode())) {
                throw exception;
            }
        }
    }

    private void recordGenerationEvent(
            String statementId,
            String accountId,
            String periodYearMonth,
            StatementGenerationEventType eventType,
            StatementEventStatus status,
            String reasonCode,
            String metadata) {
        StatementGenerationEvent event = new StatementGenerationEvent();
        event.setStatementId(statementId);
        event.setAccountId(accountId);
        event.setPeriodYearMonth(periodYearMonth);
        event.setEventType(eventType);
        event.setStatus(status);
        event.setReasonCode(reasonCode);
        event.setMetadata(metadata == null ? "{}" : metadata);
        statementGenerationEventRepository.save(event);
    }

    private String buildArtifactUri(String statementId, int artifactVersion) {
        return statementModuleConfig.getArtifactBaseUri()
                + "/"
                + statementId
                + "/artifact/v"
                + artifactVersion
                + ".pdf";
    }

    private YearMonth normalizePeriod(String periodYearMonth) {
        if (periodYearMonth == null || periodYearMonth.isBlank()) {
            throw StatementErrors.validation("periodYearMonth is required", "periodYearMonth");
        }
        try {
            return YearMonth.parse(periodYearMonth.trim());
        } catch (DateTimeParseException exception) {
            throw StatementErrors.validation("periodYearMonth must follow YYYY-MM", "periodYearMonth");
        }
    }

    private StatementGenerationMode normalizeMode(String generationMode) {
        if (generationMode == null || generationMode.isBlank()) {
            throw StatementErrors.validation("generationMode is required", "generationMode");
        }
        try {
            return StatementGenerationMode.valueOf(generationMode.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw StatementErrors.validation("generationMode must be STANDARD or CORRECTION", "generationMode");
        }
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

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId.trim();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "unknown";
        }
        return value.replace('"', '\'').replace('\n', ' ').replace('\r', ' ');
    }
}

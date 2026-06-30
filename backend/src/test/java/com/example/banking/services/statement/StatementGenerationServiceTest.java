package com.example.banking.services.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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

class StatementGenerationServiceTest {

    private static final String ACCOUNT_ID_UPPER = "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA";
    private static final String ACCOUNT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String PERIOD = "2026-06";

    private CapturingStatementAccessGuard accessGuard;
    private CapturingStatementComputationService computationService;
    private InMemoryMonthlyStatementRepository monthlyStatementRepository;
    private InMemoryStatementActivitySummaryRepository activitySummaryRepository;
    private InMemoryStatementGenerationEventRepository generationEventRepository;
    private StatementGenerationService service;

    @BeforeEach
    void setUp() {
        accessGuard = new CapturingStatementAccessGuard();
        computationService = new CapturingStatementComputationService();
        monthlyStatementRepository = new InMemoryMonthlyStatementRepository();
        activitySummaryRepository = new InMemoryStatementActivitySummaryRepository();
        generationEventRepository = new InMemoryStatementGenerationEventRepository();

        StatementModuleConfig config = new StatementModuleConfig();
        config.setArtifactBaseUri("/statements");

        service = new StatementGenerationService(
                accessGuard,
                computationService,
                monthlyStatementRepository,
                activitySummaryRepository,
                generationEventRepository,
                config);
    }

    @Test
    void generateCreatesStandardStatementAndRecordsSuccessEvents() {
        MonthlyStatement result = service.generate(
                ACCOUNT_ID_UPPER,
                PERIOD,
                "standard",
                " actor-1 ",
                "ADMIN");

        assertEquals(MonthlyStatementStatus.GENERATED, result.getStatus());
        assertEquals(1, result.getArtifactVersion());
        assertEquals("/statements/" + result.getStatementId() + "/artifact/v1.pdf", result.getArtifactUri());
        assertEquals("USD", result.getCurrencyCode());

        assertEquals(1, activitySummaryRepository.saved.size());
        assertEquals(result.getStatementId(), activitySummaryRepository.saved.get(0).getStatementId());

        assertEquals(2, generationEventRepository.saved.size());
        assertEquals(StatementGenerationEventType.GENERATION_STARTED, generationEventRepository.saved.get(0).getEventType());
        assertEquals(StatementGenerationEventType.GENERATION_SUCCEEDED, generationEventRepository.saved.get(1).getEventType());
        assertEquals(StatementEventStatus.SUCCESS, generationEventRepository.saved.get(1).getStatus());
    }

    @Test
    void generateCreatesCorrectionStatementWhenLatestExists() {
        monthlyStatementRepository.latest = Optional.of(existingStatement(3));

        MonthlyStatement result = service.generate(
                ACCOUNT_ID,
                PERIOD,
                "CORRECTION",
                "actor-2",
                "ADMIN");

        assertEquals(4, result.getArtifactVersion());
        assertEquals(MonthlyStatementStatus.CORRECTED, result.getStatus());

        assertEquals(2, generationEventRepository.saved.size());
        assertEquals(StatementGenerationEventType.CORRECTION_GENERATED, generationEventRepository.saved.get(1).getEventType());
    }

    @Test
    void generateThrowsConflictForDuplicateStandardAndRecordsFailureEvent() {
        monthlyStatementRepository.latest = Optional.of(existingStatement(1));

        ApiErrorException conflict = captureGenerateError(ACCOUNT_ID, PERIOD, "STANDARD", "actor", "ADMIN");

        assertEquals("STATEMENT_CONFLICT", conflict.getCode());
        assertEquals("periodYearMonth", conflict.getField());

        assertEquals(2, generationEventRepository.saved.size());
        StatementGenerationEvent failed = generationEventRepository.saved.get(1);
        assertEquals(StatementGenerationEventType.GENERATION_FAILED, failed.getEventType());
        assertEquals(StatementEventStatus.FAILURE, failed.getStatus());
        assertEquals("STATEMENT_CONFLICT", failed.getReasonCode());
        assertEquals("{\"message\":\"Standard statement already exists for the provided period\"}", failed.getMetadata());
    }

    @Test
    void generateThrowsValidationForCorrectionWithoutExistingStatementAndRecordsFailureEvent() {
        monthlyStatementRepository.latest = Optional.empty();

        ApiErrorException validation = captureGenerateError(ACCOUNT_ID, PERIOD, "CORRECTION", "actor", "ADMIN");

        assertEquals("STATEMENT_VALIDATION_ERROR", validation.getCode());
        assertEquals("generationMode", validation.getField());

        assertEquals(2, generationEventRepository.saved.size());
        StatementGenerationEvent failed = generationEventRepository.saved.get(1);
        assertEquals(StatementGenerationEventType.GENERATION_FAILED, failed.getEventType());
        assertEquals("STATEMENT_VALIDATION_ERROR", failed.getReasonCode());
    }

    @Test
    void generateRethrowsApiErrorsAndSanitizesFailureMetadata() {
        ApiErrorException failure = new ApiErrorException(
                HttpStatus.BAD_REQUEST,
                "CUSTOM_ERROR",
                "bad \"line\"\nnext\rrow",
                "field");
        computationService.apiException = failure;

        ApiErrorException thrown = captureGenerateError(ACCOUNT_ID, PERIOD, "STANDARD", "actor", "ADMIN");

        assertSame(failure, thrown);

        assertEquals(2, generationEventRepository.saved.size());
        StatementGenerationEvent failed = generationEventRepository.saved.get(1);
        assertEquals("CUSTOM_ERROR", failed.getReasonCode());
        assertEquals("{\"message\":\"bad 'line' next row\"}", failed.getMetadata());
    }

    @Test
    void generateWrapsUnexpectedExceptionAsDependencyFailureAndUsesUnknownMessageWhenNull() {
        computationService.runtimeException = new RuntimeException((String) null);

        ApiErrorException dependencyFailure = captureGenerateError(ACCOUNT_ID, PERIOD, "STANDARD", "actor", "ADMIN");

        assertEquals("STATEMENT_DEPENDENCY_FAILURE", dependencyFailure.getCode());

        assertEquals(2, generationEventRepository.saved.size());
        StatementGenerationEvent failed = generationEventRepository.saved.get(1);
        assertEquals("STATEMENT_DEPENDENCY_FAILURE", failed.getReasonCode());
        assertEquals("{\"message\":\"unknown\"}", failed.getMetadata());
    }

    @Test
    void generateValidatesAccountIdInput() {
        ApiErrorException missing = captureGenerateError(null, PERIOD, "STANDARD", "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", missing.getCode());
        assertEquals("accountId", missing.getField());

        ApiErrorException blank = captureGenerateError("   ", PERIOD, "STANDARD", "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", blank.getCode());
        assertEquals("accountId", blank.getField());

        ApiErrorException invalid = captureGenerateError("not-a-uuid", PERIOD, "STANDARD", "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", invalid.getCode());
        assertEquals("accountId", invalid.getField());
    }

    @Test
    void generateValidatesPeriodInput() {
        ApiErrorException missingNull = captureGenerateError(ACCOUNT_ID, null, "STANDARD", "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", missingNull.getCode());
        assertEquals("periodYearMonth", missingNull.getField());

        ApiErrorException missing = captureGenerateError(ACCOUNT_ID, " ", "STANDARD", "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", missing.getCode());
        assertEquals("periodYearMonth", missing.getField());

        ApiErrorException invalid = captureGenerateError(ACCOUNT_ID, "202606", "STANDARD", "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", invalid.getCode());
        assertEquals("periodYearMonth", invalid.getField());
    }

    @Test
    void generateValidatesGenerationModeInput() {
        ApiErrorException missingNull = captureGenerateError(ACCOUNT_ID, PERIOD, null, "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", missingNull.getCode());
        assertEquals("generationMode", missingNull.getField());

        ApiErrorException missing = captureGenerateError(ACCOUNT_ID, PERIOD, " ", "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", missing.getCode());
        assertEquals("generationMode", missing.getField());

        ApiErrorException invalid = captureGenerateError(ACCOUNT_ID, PERIOD, "SNAPSHOT", "actor", "ADMIN");
        assertEquals("STATEMENT_VALIDATION_ERROR", invalid.getCode());
        assertEquals("generationMode", invalid.getField());
    }

    @Test
    void generateNormalizesRolePrefixAndBlankActorBeforeScopeCheck() {
        service.generate(ACCOUNT_ID_UPPER, PERIOD, "STANDARD", "  ", "ROLE_ADMIN");

        assertEquals("ADMIN", accessGuard.lastScopedRole);
        assertEquals("anonymous", accessGuard.lastScopedActor);
        assertEquals(ACCOUNT_ID, accessGuard.lastScopedAccountId);
    }

    @Test
    void generateNormalizesNullRoleAndNullActorBeforeScopeCheck() {
        service.generate(ACCOUNT_ID_UPPER, PERIOD, "STANDARD", null, null);

        assertEquals("", accessGuard.lastScopedRole);
        assertEquals("anonymous", accessGuard.lastScopedActor);
    }

    @Test
    void generateForSchedulerSwallowsConflictAndUsesSystemIdentity() {
        monthlyStatementRepository.latest = Optional.of(existingStatement(1));

        assertNull(captureGenerateForSchedulerError(ACCOUNT_ID_UPPER, PERIOD));

        assertEquals("ADMIN", accessGuard.lastEnforcedRole);
        assertEquals("ADMIN", accessGuard.lastScopedRole);
        assertEquals("system-scheduler", accessGuard.lastScopedActor);
    }

    @Test
    void generateForSchedulerRunsSuccessfullyWhenNoConflictExists() {
        monthlyStatementRepository.latest = Optional.empty();

        assertNull(captureGenerateForSchedulerError(ACCOUNT_ID, PERIOD));
        assertEquals("ADMIN", accessGuard.lastEnforcedRole);
    }

    @Test
    void generateForSchedulerRethrowsNonConflictApiErrors() {
        accessGuard.enforceException = StatementErrors.forbidden("generate");

        ApiErrorException forbidden = captureGenerateForSchedulerError(ACCOUNT_ID, PERIOD);

        assertEquals("STATEMENT_FORBIDDEN", forbidden.getCode());
    }

    @Test
    void generateRethrowsScopeErrorsBeforeGenerationEventsAreWritten() {
        accessGuard.scopeException = StatementErrors.forbidden("generate");

        ApiErrorException forbidden = captureGenerateError(ACCOUNT_ID, PERIOD, "STANDARD", "actor", "ADMIN");

        assertEquals("STATEMENT_FORBIDDEN", forbidden.getCode());
        assertEquals(0, generationEventRepository.saved.size());
    }

    @Test
    void inMemoryMonthlyStatementRepositoryMethodsAreCovered() {
        InMemoryMonthlyStatementRepository repository = new InMemoryMonthlyStatementRepository();
        MonthlyStatement statement = existingStatement(2);
        repository.latest = Optional.of(statement);

        assertSame(statement, repository.save(statement));
        assertTrue(repository.findById("missing").isEmpty());
        assertSame(statement, repository.findLatestByAccountAndPeriod(ACCOUNT_ID, PERIOD).orElseThrow());
        assertTrue(repository.listByScope("actor", "ADMIN", ACCOUNT_ID, PERIOD, 1, 10).isEmpty());
    }

    @Test
    void inMemoryActivitySummaryAndGenerationEventRepositoryMethodsAreCovered() {
        InMemoryStatementActivitySummaryRepository summaryRepository = new InMemoryStatementActivitySummaryRepository();
        InMemoryStatementGenerationEventRepository eventRepository = new InMemoryStatementGenerationEventRepository();

        StatementActivitySummary summary = new StatementActivitySummary();
        summary.setStatementId("stmt-1");
        assertSame(summary, summaryRepository.save(summary));
        assertSame(summary, summaryRepository.findByStatementId("stmt-1").orElseThrow());
        assertTrue(summaryRepository.findByStatementId("missing").isEmpty());

        StatementGenerationEvent event = new StatementGenerationEvent();
        event.setStatementId("stmt-1");
        event.setMetadata("{}");
        assertSame(event, eventRepository.save(event));
        assertEquals(1, eventRepository.saved.size());
        assertSame(event, eventRepository.saved.get(0));
    }

        @Test
        void recordGenerationEventDefaultsMetadataToEmptyObjectWhenNull() throws Exception {
        Method method = StatementGenerationService.class.getDeclaredMethod(
            "recordGenerationEvent",
            String.class,
            String.class,
            String.class,
            StatementGenerationEventType.class,
            StatementEventStatus.class,
            String.class,
            String.class);
        method.setAccessible(true);

        method.invoke(
            service,
            null,
            ACCOUNT_ID,
            PERIOD,
            StatementGenerationEventType.GENERATION_STARTED,
            StatementEventStatus.SUCCESS,
            null,
            null);

        assertEquals(1, generationEventRepository.saved.size());
        assertEquals("{}", generationEventRepository.saved.get(0).getMetadata());
        }

    private MonthlyStatement existingStatement(int artifactVersion) {
        MonthlyStatement statement = new MonthlyStatement();
        statement.setArtifactVersion(artifactVersion);
        statement.setPeriodYearMonth(PERIOD);
        statement.setAccountId(ACCOUNT_ID);
        return statement;
    }

    private ApiErrorException captureGenerateError(
            String accountId,
            String periodYearMonth,
            String generationMode,
            String actorUserId,
            String role) {
        ApiErrorException exception = null;
        try {
            service.generate(accountId, periodYearMonth, generationMode, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private ApiErrorException captureGenerateForSchedulerError(String accountId, String periodYearMonth) {
        ApiErrorException exception = null;
        try {
            service.generateForScheduler(accountId, periodYearMonth);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private static final class CapturingStatementAccessGuard extends StatementAccessGuard {
        private ApiErrorException enforceException;
        private ApiErrorException scopeException;
        private String lastEnforcedRole;
        private String lastScopedAccountId;
        private String lastScopedRole;
        private String lastScopedActor;

        private CapturingStatementAccessGuard() {
            super(null, null);
        }

        @Override
        public void enforceGenerationAccess(String role) {
            lastEnforcedRole = role;
            if (enforceException != null) {
                throw enforceException;
            }
        }

        @Override
        public AccountEntity requireAccountScope(String accountId, String role, String actorUserId, String operation) {
            lastScopedAccountId = accountId;
            lastScopedRole = role;
            lastScopedActor = actorUserId;
            if (scopeException != null) {
                throw scopeException;
            }

            AccountEntity account = new AccountEntity();
            account.setAccountId(accountId);
            account.setCurrencyCode("USD");
            return account;
        }
    }

    private static final class CapturingStatementComputationService extends StatementComputationService {
        private ApiErrorException apiException;
        private RuntimeException runtimeException;

        private CapturingStatementComputationService() {
            super(null);
        }

        @Override
        public ComputationResult compute(String accountId, Instant periodStartUtc, Instant periodEndUtc) {
            if (apiException != null) {
                throw apiException;
            }
            if (runtimeException != null) {
                throw runtimeException;
            }
            return new ComputationResult(
                    new BigDecimal("100.00"),
                    new BigDecimal("125.00"),
                    new BigDecimal("25.00"),
                    new BigDecimal("50.00"),
                    3);
        }
    }

    private static final class InMemoryMonthlyStatementRepository implements MonthlyStatementRepository {
        private Optional<MonthlyStatement> latest = Optional.empty();

        @Override
        public MonthlyStatement save(MonthlyStatement statement) {
            return statement;
        }

        @Override
        public Optional<MonthlyStatement> findById(String statementId) {
            return Optional.empty();
        }

        @Override
        public Optional<MonthlyStatement> findLatestByAccountAndPeriod(String accountId, String periodYearMonth) {
            return latest;
        }

        @Override
        public org.springframework.data.domain.Page<MonthlyStatement> listByScope(
                String actorUserId,
                String role,
                String accountId,
                String periodYearMonth,
                int page,
                int pageSize) {
            return org.springframework.data.domain.Page.empty();
        }
    }

    private static final class InMemoryStatementActivitySummaryRepository implements StatementActivitySummaryRepository {
        private final List<StatementActivitySummary> saved = new ArrayList<>();

        @Override
        public StatementActivitySummary save(StatementActivitySummary summary) {
            saved.add(summary);
            return summary;
        }

        @Override
        public Optional<StatementActivitySummary> findByStatementId(String statementId) {
            return saved.stream().filter(item -> statementId.equals(item.getStatementId())).findFirst();
        }
    }

    private static final class InMemoryStatementGenerationEventRepository implements StatementGenerationEventRepository {
        private final List<StatementGenerationEvent> saved = new ArrayList<>();

        @Override
        public StatementGenerationEvent save(StatementGenerationEvent event) {
            saved.add(event);
            return event;
        }
    }
}

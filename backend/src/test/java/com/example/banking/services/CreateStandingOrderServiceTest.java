package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.standingorders.schemas.CreateStandingOrderSchema;
import com.example.banking.lib.config.StandingOrderModuleConfig;
import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.lib.errors.StandingOrderErrors;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.lib.scheduling.StandingOrderScheduleCalculator;
import com.example.banking.lib.security.StandingOrderAccessPolicy;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.StandingOrderCadence;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleState;

class CreateStandingOrderServiceTest {

    private static final String SOURCE_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String DESTINATION_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

    private CapturingStandingOrderAccessPolicy accessPolicy;
    private CapturingScheduleCalculator scheduleCalculator;
    private InMemoryStandingOrderRepository standingOrderRepository;
    private CapturingAuditService auditService;
    private CreateStandingOrderService service;

    @BeforeEach
    void setUp() {
        accessPolicy = new CapturingStandingOrderAccessPolicy();
        scheduleCalculator = new CapturingScheduleCalculator();
        standingOrderRepository = new InMemoryStandingOrderRepository();
        auditService = new CapturingAuditService();

        TransactionModuleConfig transactionModuleConfig = new TransactionModuleConfig();
        transactionModuleConfig.setDefaultCurrencyCode("USD");
        MoneyPolicy moneyPolicy = new MoneyPolicy(transactionModuleConfig);

        StandingOrderModuleConfig standingOrderModuleConfig = new StandingOrderModuleConfig();
        standingOrderModuleConfig.setDefaultRetryPolicyCode("STANDARD");

        service = new CreateStandingOrderService(
                moneyPolicy,
                accessPolicy,
                scheduleCalculator,
                standingOrderRepository,
                auditService,
                standingOrderModuleConfig);

        accessPolicy.sourceAccount = account(SOURCE_ID, "aCtIvE", "USD");
        accessPolicy.destinationAccount = account(DESTINATION_ID, "ACTIVE", "USD");
        scheduleCalculator.nextExecutionAt = Instant.parse("2026-07-02T00:00:00Z");
    }

    @Test
    void createSuccessPathNormalizesAndPersistsExpectedFields() {
        CreateStandingOrderSchema request = new CreateStandingOrderSchema(
                SOURCE_ID.toUpperCase(),
                DESTINATION_ID.toUpperCase(),
                "10.005",
                " weekly ",
                " 2026-07-01T00:00:00Z ",
                " 2026-07-31T00:00:00Z ",
                " no_retry ");

        StandingOrderEntity created = service.create(request, null, "CUSTOMER");

        assertNotNull(created.getStandingOrderId());
        assertEquals(SOURCE_ID, created.getSourceAccountId());
        assertEquals(DESTINATION_ID, created.getDestinationAccountId());
        assertEquals(new BigDecimal("10.00"), created.getAmount());
        assertEquals("USD", created.getCurrencyCode());
        assertEquals(StandingOrderCadence.WEEKLY, created.getCadence());
        assertEquals("{\"timezone\":\"UTC\"}", created.getScheduleConfig());
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), created.getEffectiveFromUtc());
        assertEquals(Instant.parse("2026-07-31T00:00:00Z"), created.getEffectiveToUtc());
        assertEquals(Instant.parse("2026-07-02T00:00:00Z"), created.getNextExecutionAtUtc());
        assertEquals(StandingOrderLifecycleState.ACTIVE, created.getLifecycleState());
        assertEquals("NO_RETRY", created.getRetryPolicyCode());
        assertEquals("anonymous", created.getCreatedByUserId());
        assertNotNull(created.getUpdatedAtUtc());

        assertEquals(2, accessPolicy.calls.size());
        assertEquals(SOURCE_ID, accessPolicy.calls.get(0).accountId());
        assertEquals("sourceAccountId", accessPolicy.calls.get(0).field());
        assertEquals("anonymous", accessPolicy.calls.get(0).actorUserId());
        assertEquals("CUSTOMER", accessPolicy.calls.get(0).role());
        assertEquals(DESTINATION_ID, accessPolicy.calls.get(1).accountId());
        assertEquals("destinationAccountId", accessPolicy.calls.get(1).field());

        assertEquals(1, standingOrderRepository.saved.size());
        assertEquals(1, auditService.calls.size());
        assertEquals("CREATED", auditService.calls.get(0).eventType());
        assertEquals("anonymous", auditService.calls.get(0).actorUserId());
        assertEquals("CUSTOMER", auditService.calls.get(0).actorRole());
        assertTrue(auditService.calls.get(0).metadata().contains(SOURCE_ID));
        assertTrue(auditService.calls.get(0).metadata().contains(DESTINATION_ID));

        assertEquals(StandingOrderCadence.WEEKLY, scheduleCalculator.lastCadence);
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), scheduleCalculator.lastEffectiveFromUtc);
        assertEquals(Instant.parse("2026-07-31T00:00:00Z"), scheduleCalculator.lastEffectiveToUtc);
        assertNotNull(scheduleCalculator.lastNowUtc);
    }

    @Test
    void createUsesDefaultRetryPolicyAndNullOptionalToWhenBlank() {
        CreateStandingOrderSchema request = request(
                SOURCE_ID,
                DESTINATION_ID,
                "25.00",
                "DAILY",
                "2026-07-01T00:00:00Z",
                "   ",
                " ");

        StandingOrderEntity created = service.create(request, "actor-1", "ADMIN");

        assertEquals("STANDARD", created.getRetryPolicyCode());
        assertNull(created.getEffectiveToUtc());
        assertEquals("actor-1", created.getCreatedByUserId());
        assertEquals("actor-1", accessPolicy.calls.get(0).actorUserId());
    }

    @Test
    void createRejectsWhenSourceAndDestinationMatch() {
        CreateStandingOrderSchema request = request(
                SOURCE_ID,
                SOURCE_ID,
                "10.00",
                "DAILY",
                "2026-07-01T00:00:00Z",
                null,
                "STANDARD");

        ApiErrorException exception = captureCreateError(request, "actor-1", "CUSTOMER");

        assertNotNull(exception);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", exception.getCode());
        assertEquals("destinationAccountId", exception.getField());
    }

    @Test
    void createRejectsBlankOrInvalidSourceAccountId() {
        ApiErrorException blank = captureCreateError(
                request(" ", DESTINATION_ID, "10.00", "DAILY", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(blank);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", blank.getCode());
        assertEquals("sourceAccountId", blank.getField());

        ApiErrorException invalid = captureCreateError(
                request("not-a-uuid", DESTINATION_ID, "10.00", "DAILY", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(invalid);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", invalid.getCode());
        assertEquals("sourceAccountId", invalid.getField());
    }

    @Test
    void createRejectsMissingOrInvalidDestinationAccountId() {
        ApiErrorException missing = captureCreateError(
                request(SOURCE_ID, null, "10.00", "DAILY", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(missing);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", missing.getCode());
        assertEquals("destinationAccountId", missing.getField());
        assertEquals(1, accessPolicy.calls.size());

        accessPolicy.calls.clear();
        ApiErrorException invalid = captureCreateError(
                request(SOURCE_ID, "bad-id", "10.00", "DAILY", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(invalid);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", invalid.getCode());
        assertEquals("destinationAccountId", invalid.getField());
    }

    @Test
    void createRejectsWhenSourceAccountIsNotActive() {
        accessPolicy.sourceAccount = account(SOURCE_ID, null, "USD");

        ApiErrorException exception = captureCreateError(baseRequest(), "actor-1", "CUSTOMER");

        assertNotNull(exception);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", exception.getCode());
        assertEquals("sourceAccountId", exception.getField());
    }

    @Test
    void createRejectsWhenDestinationAccountIsNotActive() {
        accessPolicy.destinationAccount = account(DESTINATION_ID, "SUSPENDED", "USD");

        ApiErrorException exception = captureCreateError(baseRequest(), "actor-1", "CUSTOMER");

        assertNotNull(exception);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", exception.getCode());
        assertEquals("destinationAccountId", exception.getField());
    }

    @Test
    void createRejectsWhenCurrenciesDoNotMatch() {
        accessPolicy.destinationAccount = account(DESTINATION_ID, "ACTIVE", "EUR");

        ApiErrorException exception = captureCreateError(baseRequest(), "actor-1", "CUSTOMER");

        assertNotNull(exception);
        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("currencyCode", exception.getField());
    }

    @Test
    void createRejectsInvalidAmountVariants() {
        ApiErrorException missing = captureCreateError(
                request(SOURCE_ID, DESTINATION_ID, " ", "DAILY", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(missing);
        assertEquals("TRANSACTION_VALIDATION_ERROR", missing.getCode());
        assertEquals("amount", missing.getField());

        ApiErrorException malformed = captureCreateError(
                request(SOURCE_ID, DESTINATION_ID, "abc", "DAILY", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(malformed);
        assertEquals("TRANSACTION_VALIDATION_ERROR", malformed.getCode());
        assertEquals("amount", malformed.getField());

        ApiErrorException nonPositive = captureCreateError(
                request(SOURCE_ID, DESTINATION_ID, "0.00", "DAILY", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(nonPositive);
        assertEquals("TRANSACTION_VALIDATION_ERROR", nonPositive.getCode());
        assertEquals("amount", nonPositive.getField());
    }

    @Test
    void createRejectsCadenceValidationBranches() {
        ApiErrorException missing = captureCreateError(
                request(SOURCE_ID, DESTINATION_ID, "10.00", " ", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(missing);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", missing.getCode());
        assertEquals("cadence", missing.getField());

        ApiErrorException invalid = captureCreateError(
                request(SOURCE_ID, DESTINATION_ID, "10.00", "YEARLY", "2026-07-01T00:00:00Z", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(invalid);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", invalid.getCode());
        assertEquals("cadence", invalid.getField());
    }

    @Test
    void createRejectsEffectiveFromValidationBranches() {
        ApiErrorException missing = captureCreateError(
                request(SOURCE_ID, DESTINATION_ID, "10.00", "DAILY", " ", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(missing);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", missing.getCode());
        assertEquals("effectiveFromUtc", missing.getField());

        ApiErrorException invalid = captureCreateError(
                request(SOURCE_ID, DESTINATION_ID, "10.00", "DAILY", "bad-date", null, "STANDARD"),
                "actor-1",
                "CUSTOMER");
        assertNotNull(invalid);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", invalid.getCode());
        assertEquals("effectiveFromUtc", invalid.getField());
    }

    @Test
    void createRejectsInvalidEffectiveTo() {
        ApiErrorException invalid = captureCreateError(
                request(SOURCE_ID, DESTINATION_ID, "10.00", "DAILY", "2026-07-01T00:00:00Z", "bad-date", "STANDARD"),
                "actor-1",
                "CUSTOMER");

        assertNotNull(invalid);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", invalid.getCode());
        assertEquals("effectiveToUtc", invalid.getField());
    }

    @Test
    void createRejectsWhenScheduleHasNoExecutableOccurrence() {
        scheduleCalculator.nextExecutionAt = null;

        ApiErrorException exception = captureCreateError(baseRequest(), "actor-1", "CUSTOMER");

        assertNotNull(exception);
        assertEquals("STANDING_ORDER_VALIDATION_ERROR", exception.getCode());
        assertEquals("effectiveToUtc", exception.getField());
    }

    @Test
    void createPropagatesAccessPolicyException() {
        accessPolicy.exception = StandingOrderErrors.forbidden("manage");

        ApiErrorException exception = captureCreateError(baseRequest(), "actor-1", "CUSTOMER");

        assertNotNull(exception);
        assertEquals("STANDING_ORDER_FORBIDDEN", exception.getCode());
        assertEquals(1, accessPolicy.calls.size());
        assertEquals("sourceAccountId", accessPolicy.calls.get(0).field());
        assertEquals(0, standingOrderRepository.saved.size());
        assertEquals(0, auditService.calls.size());
    }

    private CreateStandingOrderSchema baseRequest() {
        return request(
                SOURCE_ID,
                DESTINATION_ID,
                "10.00",
                "DAILY",
                "2026-07-01T00:00:00Z",
                "2026-07-31T00:00:00Z",
                "STANDARD");
    }

    private CreateStandingOrderSchema request(
            String sourceAccountId,
            String destinationAccountId,
            String amount,
            String cadence,
            String effectiveFromUtc,
            String effectiveToUtc,
            String retryPolicyCode) {
        return new CreateStandingOrderSchema(
                sourceAccountId,
                destinationAccountId,
                amount,
                cadence,
                effectiveFromUtc,
                effectiveToUtc,
                retryPolicyCode);
    }

    private ApiErrorException captureCreateError(CreateStandingOrderSchema request, String actorUserId, String role) {
        ApiErrorException exception = null;
        try {
            service.create(request, actorUserId, role);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private AccountEntity account(String accountId, String status, String currencyCode) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setCustomerId(UUID.randomUUID().toString());
        account.setAccountNumber("NB1234567890");
        account.setAccountType("CHECKING");
        account.setStatus(status);
        account.setBalance(new BigDecimal("100.00"));
        account.setCurrencyCode(currencyCode);
        account.setCreatedByUserId("creator");
        account.setOwnerUserId("owner");
        account.setOpenedAtUtc(Instant.parse("2026-01-01T00:00:00Z"));
        account.setUpdatedAtUtc(Instant.parse("2026-01-01T00:00:00Z"));
        return account;
    }

    private record AccessPolicyCall(String accountId, String role, String actorUserId, String field) {
    }

    private record AuditCall(
            String standingOrderId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
    }

    private static final class CapturingStandingOrderAccessPolicy extends StandingOrderAccessPolicy {
        private final List<AccessPolicyCall> calls = new ArrayList<>();
        private AccountEntity sourceAccount;
        private AccountEntity destinationAccount;
        private ApiErrorException exception;

        private CapturingStandingOrderAccessPolicy() {
            super(null, null);
        }

        @Override
        public AccountEntity requireAccountScope(String accountId, String role, String actorUserId, String field) {
            calls.add(new AccessPolicyCall(accountId, role, actorUserId, field));
            if (exception != null) {
                throw exception;
            }
            if ("sourceAccountId".equals(field)) {
                return sourceAccount;
            }
            return destinationAccount;
        }
    }

    private static final class CapturingScheduleCalculator extends StandingOrderScheduleCalculator {
        private Instant nextExecutionAt;
        private Instant lastEffectiveFromUtc;
        private Instant lastEffectiveToUtc;
        private StandingOrderCadence lastCadence;
        private Instant lastNowUtc;

        @Override
        public Instant calculateInitialNextExecutionAt(
                Instant effectiveFromUtc,
                Instant effectiveToUtc,
                StandingOrderCadence cadence,
                Instant nowUtc) {
            lastEffectiveFromUtc = effectiveFromUtc;
            lastEffectiveToUtc = effectiveToUtc;
            lastCadence = cadence;
            lastNowUtc = nowUtc;
            return nextExecutionAt;
        }
    }

    private static final class InMemoryStandingOrderRepository implements StandingOrderRepository {
        private final List<StandingOrderEntity> saved = new ArrayList<>();

        @Override
        public StandingOrderEntity save(StandingOrderEntity standingOrder) {
            saved.add(standingOrder);
            return standingOrder;
        }

        @Override
        public Optional<StandingOrderEntity> findById(String standingOrderId) {
            return saved.stream().filter(item -> standingOrderId.equals(item.getStandingOrderId())).findFirst();
        }

        @Override
        public List<StandingOrderEntity> findDueWithinWindow(Instant windowStartUtc, Instant windowEndUtc) {
            return List.of();
        }

        @Override
        public Page<StandingOrderEntity> listByScope(String actorUserId, String role, int page, int pageSize) {
            return Page.empty();
        }
    }

    private static final class CapturingAuditService extends StandingOrderLifecycleAuditService {
        private final List<AuditCall> calls = new ArrayList<>();

        private CapturingAuditService() {
            super(null);
        }

        @Override
        public void recordEvent(
                String standingOrderId,
                String eventType,
                String actorUserId,
                String actorRole,
                String reasonCode,
                String metadata) {
            calls.add(new AuditCall(standingOrderId, eventType, actorUserId, actorRole, reasonCode, metadata));
        }
    }
}
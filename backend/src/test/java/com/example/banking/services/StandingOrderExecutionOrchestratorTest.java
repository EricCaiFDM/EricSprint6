package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.config.StandingOrderModuleConfig;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.StandingOrderCadence;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderExecutionEventEntity;
import com.example.banking.models.StandingOrderExecutionStatus;
import com.example.banking.models.StandingOrderLifecycleState;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
import com.example.banking.models.TransferLinkEntity;

class StandingOrderExecutionOrchestratorTest {

    private InMemoryStandingOrderRepository standingOrderRepository;
    private InMemoryStandingOrderExecutionEventRepository executionEventRepository;
    private CapturingStandingOrderRetryPolicyService retryPolicyService;
    private CapturingStandingOrderScheduleCalculatorBridge scheduleCalculatorBridge;
    private CapturingBalanceConsistencyService balanceConsistencyService;
    private InMemoryTransactionRepository transactionRepository;
    private CapturingTransferLinkService transferLinkService;
    private CapturingStandingOrderLifecycleAuditService lifecycleAuditService;
    private CapturingNotificationAutoTriggerService notificationAutoTriggerService;
    private StandingOrderExecutionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        standingOrderRepository = new InMemoryStandingOrderRepository();
        executionEventRepository = new InMemoryStandingOrderExecutionEventRepository();
        retryPolicyService = new CapturingStandingOrderRetryPolicyService();
        scheduleCalculatorBridge = new CapturingStandingOrderScheduleCalculatorBridge();
        balanceConsistencyService = new CapturingBalanceConsistencyService();
        transactionRepository = new InMemoryTransactionRepository();
        transferLinkService = new CapturingTransferLinkService();
        lifecycleAuditService = new CapturingStandingOrderLifecycleAuditService();
        notificationAutoTriggerService = new CapturingNotificationAutoTriggerService();

        AccountEntity sourceAccount = account("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "USD", "200.00");
        AccountEntity destinationAccount = account("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "USD", "50.00");

        balanceConsistencyService.lockedPair = new BalanceConsistencyService.LockedAccountPair(sourceAccount, destinationAccount);
        balanceConsistencyService.debitMutation = new BalanceConsistencyService.BalanceMutation(
                sourceAccount,
                new BigDecimal("200.00"),
                new BigDecimal("150.00"));
        balanceConsistencyService.creditMutation = new BalanceConsistencyService.BalanceMutation(
                destinationAccount,
                new BigDecimal("50.00"),
                new BigDecimal("100.00"));

        retryPolicyService.nextDecision = new StandingOrderRetryPolicyService.RetryDecision(false, null, "NO_RETRY", "STANDARD");
        scheduleCalculatorBridge.nextExecutionAt = Instant.parse("2026-07-02T00:00:00Z");

        TransferLinkEntity transfer = new TransferLinkEntity();
        transfer.setTransferId("transfer-1");
        transferLinkService.transferToReturn = transfer;

        orchestrator = new StandingOrderExecutionOrchestrator(
                standingOrderRepository,
                executionEventRepository,
                retryPolicyService,
                scheduleCalculatorBridge,
                balanceConsistencyService,
                transactionRepository,
                transferLinkService,
                lifecycleAuditService,
                notificationAutoTriggerService);
    }

    @Test
    void processWindowCountsSucceededRetriedAndFailedOutcomes() {
        standingOrderRepository.dueOrders = List.of(
                standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z")),
                standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:10:00Z")),
                standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:20:00Z")));

        StandingOrderExecutionOrchestrator summaryOrchestrator = new SequencedOutcomeOrchestrator(
                standingOrderRepository,
                List.of(
                        StandingOrderExecutionStatus.SUCCEEDED,
                        StandingOrderExecutionStatus.RETRY_SCHEDULED,
                        StandingOrderExecutionStatus.FAILED_DEPENDENCY_OUTAGE));

        StandingOrderExecutionOrchestrator.ExecutionSummary summary = summaryOrchestrator.processWindow(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T01:00:00Z"));

        assertEquals(3, summary.processed());
        assertEquals(1, summary.succeeded());
        assertEquals(1, summary.retried());
        assertEquals(1, summary.failed());
    }

    @Test
    void executeStandingOrderSucceedsAndSchedulesNextExecution() {
        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.SUCCEEDED, outcome.status());
        assertNotNull(outcome.executionEventId());
        assertEquals(scheduleCalculatorBridge.nextExecutionAt, standingOrder.getNextExecutionAtUtc());
        assertEquals(1, standingOrderRepository.savedOrders.size());

        assertEquals(1, executionEventRepository.savedEvents.size());
        StandingOrderExecutionEventEntity event = executionEventRepository.savedEvents.get(0);
        assertEquals(StandingOrderExecutionStatus.SUCCEEDED, event.getStatus());
        assertEquals("transfer-1", event.getTransferReferenceId());
        assertNull(event.getReasonCode());

        assertEquals(1, notificationAutoTriggerService.calls.size());
        assertEquals(0, lifecycleAuditService.calls.size());

        assertEquals(2, transactionRepository.lastSavedTransactions.size());
        assertTrue(transactionRepository.lastSavedTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.TRANSFER_DEBIT)
            .findFirst()
            .isPresent());
        assertTrue(transactionRepository.lastSavedTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.TRANSFER_CREDIT)
                .findFirst()
            .isPresent());
    }

    @Test
    void executeStandingOrderSucceedsAndCompletesWhenNoFutureExecutionExists() {
        scheduleCalculatorBridge.nextExecutionAt = null;
        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.SUCCEEDED, outcome.status());
        assertNull(standingOrder.getNextExecutionAtUtc());
        assertEquals(StandingOrderLifecycleState.COMPLETED, standingOrder.getLifecycleState());

        assertEquals(1, lifecycleAuditService.calls.size());
        LifecycleAuditCall call = lifecycleAuditService.calls.get(0);
        assertEquals("COMPLETED", call.eventType());
        assertNull(call.reasonCode());
    }

    @Test
    void executeStandingOrderApiFailureCanScheduleRetry() {
        balanceConsistencyService.apiException = apiError(
                "TRANSACTION_INSUFFICIENT_FUNDS",
                "insufficient \"funds\"\nretry");
        Instant retryAt = Instant.parse("2026-07-01T02:00:00Z");
        retryPolicyService.nextDecision = new StandingOrderRetryPolicyService.RetryDecision(true, retryAt, "RETRY_SCHEDULED", "STANDARD");

        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), null);

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.RETRY_SCHEDULED, outcome.status());
        assertEquals("FAILED_INSUFFICIENT_FUNDS", retryPolicyService.lastFailureReasonCode);
        assertEquals(retryAt, standingOrder.getNextExecutionAtUtc());

        StandingOrderExecutionEventEntity event = lastExecutionEvent();
        assertEquals(StandingOrderExecutionStatus.RETRY_SCHEDULED, event.getStatus());
        assertEquals("TRANSACTION_INSUFFICIENT_FUNDS", event.getReasonCode());
        assertEquals(retryAt, event.getNextRetryAtUtc());
        assertEquals("{\"message\":\"insufficient 'funds' retry\"}", event.getMetadata());

        assertEquals(0, lifecycleAuditService.calls.size());
        assertEquals(0, notificationAutoTriggerService.calls.size());
    }

    @Test
    void executeStandingOrderApiFailureIneligibleFromTransactionAccountNotFoundCompletesWhenScheduleEnds() {
        balanceConsistencyService.apiException = apiError(
                "TRANSACTION_ACCOUNT_NOT_FOUND",
                "account missing");
        retryPolicyService.nextDecision = new StandingOrderRetryPolicyService.RetryDecision(false, null, "NO_RETRY", "STANDARD");
        scheduleCalculatorBridge.nextExecutionAt = null;

        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.FAILED_INELIGIBLE_ACCOUNT, outcome.status());
        assertEquals("FAILED_INELIGIBLE_ACCOUNT", retryPolicyService.lastFailureReasonCode);
        assertEquals(StandingOrderLifecycleState.COMPLETED, standingOrder.getLifecycleState());

        assertEquals(1, lifecycleAuditService.calls.size());
        assertEquals("TRANSACTION_ACCOUNT_NOT_FOUND", lifecycleAuditService.calls.get(0).reasonCode());
    }

    @Test
    void executeStandingOrderApiFailureIneligibleFromStandingOrderAccountNotFoundCode() {
        balanceConsistencyService.apiException = apiError(
                "STANDING_ORDER_ACCOUNT_NOT_FOUND",
                "soa missing");
        retryPolicyService.nextDecision = new StandingOrderRetryPolicyService.RetryDecision(false, null, "NO_RETRY", "STANDARD");
        scheduleCalculatorBridge.nextExecutionAt = Instant.parse("2026-07-03T00:00:00Z");

        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.FAILED_INELIGIBLE_ACCOUNT, outcome.status());
        assertEquals("FAILED_INELIGIBLE_ACCOUNT", retryPolicyService.lastFailureReasonCode);
        assertEquals(0, lifecycleAuditService.calls.size());
    }

    @Test
    void executeStandingOrderApiFailureIneligibleFromTransactionConflictCode() {
        balanceConsistencyService.apiException = apiError(
                "TRANSACTION_CONFLICT",
                "conflict");
        retryPolicyService.nextDecision = new StandingOrderRetryPolicyService.RetryDecision(false, null, "NO_RETRY", "STANDARD");

        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.FAILED_INELIGIBLE_ACCOUNT, outcome.status());
        assertEquals("FAILED_INELIGIBLE_ACCOUNT", retryPolicyService.lastFailureReasonCode);
    }

    @Test
    void executeStandingOrderUnexpectedExceptionMapsToDependencyOutageAndUsesUnknownMessage() {
        transferLinkService.runtimeException = new RuntimeException((String) null);
        retryPolicyService.nextDecision = new StandingOrderRetryPolicyService.RetryDecision(false, null, "NO_RETRY", "STANDARD");
        scheduleCalculatorBridge.nextExecutionAt = Instant.parse("2026-07-04T00:00:00Z");

        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.FAILED_DEPENDENCY_OUTAGE, outcome.status());
        assertEquals("FAILED_DEPENDENCY_OUTAGE", retryPolicyService.lastFailureReasonCode);

        StandingOrderExecutionEventEntity event = lastExecutionEvent();
        assertEquals("FAILED_DEPENDENCY_OUTAGE", event.getReasonCode());
        assertEquals("{\"message\":\"unknown\"}", event.getMetadata());
    }

    @Test
    void executeStandingOrderMissingDebitTransactionFallsBackToFailureHandling() {
        transactionRepository.saveAllResponse = List.of(transaction(TransactionType.TRANSFER_CREDIT, "credit-only"));
        retryPolicyService.nextDecision = new StandingOrderRetryPolicyService.RetryDecision(false, null, "NO_RETRY", "STANDARD");

        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.FAILED_DEPENDENCY_OUTAGE, outcome.status());
        assertEquals("FAILED_DEPENDENCY_OUTAGE", retryPolicyService.lastFailureReasonCode);
    }

    @Test
    void executeStandingOrderMissingCreditTransactionFallsBackToFailureHandling() {
        transactionRepository.saveAllResponse = List.of(transaction(TransactionType.TRANSFER_DEBIT, "debit-only"));
        retryPolicyService.nextDecision = new StandingOrderRetryPolicyService.RetryDecision(false, null, "NO_RETRY", "STANDARD");

        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));

        StandingOrderExecutionOrchestrator.ExecutionOutcome outcome = orchestrator.executeStandingOrder(standingOrder);

        assertEquals(StandingOrderExecutionStatus.FAILED_DEPENDENCY_OUTAGE, outcome.status());
        assertEquals("FAILED_DEPENDENCY_OUTAGE", retryPolicyService.lastFailureReasonCode);
    }

    @Test
    void saveExecutionEventDefaultsMetadataWhenNull() throws Exception {
        Method saveExecutionEvent = StandingOrderExecutionOrchestrator.class.getDeclaredMethod(
                "saveExecutionEvent",
                StandingOrderEntity.class,
                Instant.class,
                Instant.class,
                Instant.class,
                StandingOrderExecutionStatus.class,
                int.class,
                String.class,
                Instant.class,
                String.class,
                String.class);
        saveExecutionEvent.setAccessible(true);

        StandingOrderEntity standingOrder = standingOrder(UUID.randomUUID().toString(), Instant.parse("2026-07-01T00:00:00Z"));
        StandingOrderExecutionEventEntity event = (StandingOrderExecutionEventEntity) saveExecutionEvent.invoke(
                orchestrator,
                standingOrder,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:01Z"),
                Instant.parse("2026-07-01T00:00:02Z"),
                StandingOrderExecutionStatus.SUCCEEDED,
                1,
                null,
                null,
                null,
                null);

        assertEquals("{}", event.getMetadata());
        assertEquals(1, executionEventRepository.savedEvents.size());
    }

    private StandingOrderExecutionEventEntity lastExecutionEvent() {
        return executionEventRepository.savedEvents.get(executionEventRepository.savedEvents.size() - 1);
    }

    private ApiErrorException apiError(String code, String message) {
        return new ApiErrorException(HttpStatus.BAD_REQUEST, code, message, null);
    }

    private StandingOrderEntity standingOrder(String standingOrderId, Instant nextExecutionAtUtc) {
        StandingOrderEntity standingOrder = new StandingOrderEntity();
        standingOrder.setStandingOrderId(standingOrderId);
        standingOrder.setSourceAccountId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        standingOrder.setDestinationAccountId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        standingOrder.setAmount(new BigDecimal("50.00"));
        standingOrder.setCurrencyCode("USD");
        standingOrder.setCadence(StandingOrderCadence.DAILY);
        standingOrder.setLifecycleState(StandingOrderLifecycleState.ACTIVE);
        standingOrder.setRetryPolicyCode("STANDARD");
        standingOrder.setNextExecutionAtUtc(nextExecutionAtUtc);
        standingOrder.setEffectiveFromUtc(Instant.parse("2026-06-01T00:00:00Z"));
        standingOrder.setUpdatedAtUtc(Instant.parse("2026-06-30T00:00:00Z"));
        standingOrder.setCreatedByUserId("creator-1");
        return standingOrder;
    }

    private AccountEntity account(String accountId, String currencyCode, String balance) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setCurrencyCode(currencyCode);
        account.setBalance(new BigDecimal(balance));
        account.setStatus("ACTIVE");
        return account;
    }

    private TransactionEntity transaction(TransactionType type, String id) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setTransactionType(type);
        transaction.setTransactionId(id);
        return transaction;
    }

    private record LifecycleAuditCall(
            String standingOrderId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
    }

    private static final class InMemoryStandingOrderRepository implements StandingOrderRepository {
        private List<StandingOrderEntity> dueOrders = List.of();
        private final List<StandingOrderEntity> savedOrders = new ArrayList<>();

        @Override
        public StandingOrderEntity save(StandingOrderEntity standingOrder) {
            savedOrders.add(standingOrder);
            return standingOrder;
        }

        @Override
        public Optional<StandingOrderEntity> findById(String standingOrderId) {
            return Optional.empty();
        }

        @Override
        public List<StandingOrderEntity> findDueWithinWindow(Instant windowStartUtc, Instant windowEndUtc) {
            return dueOrders;
        }

        @Override
        public Page<StandingOrderEntity> listByScope(String actorUserId, String role, int page, int pageSize) {
            return Page.empty();
        }
    }

    private static final class InMemoryStandingOrderExecutionEventRepository implements StandingOrderExecutionEventRepository {
        private long countByStandingOrderId;
        private final List<StandingOrderExecutionEventEntity> savedEvents = new ArrayList<>();

        @Override
        public StandingOrderExecutionEventEntity save(StandingOrderExecutionEventEntity event) {
            savedEvents.add(event);
            return event;
        }

        @Override
        public long countByStandingOrderId(String standingOrderId) {
            return countByStandingOrderId;
        }

        @Override
        public Page<StandingOrderExecutionEventEntity> listByStandingOrderId(String standingOrderId, int page, int pageSize) {
            return Page.empty();
        }
    }

    private static final class CapturingStandingOrderRetryPolicyService extends StandingOrderRetryPolicyService {
        private RetryDecision nextDecision = new RetryDecision(false, null, "NO_RETRY", "STANDARD");
        private String lastPolicyCode;
        private String lastFailureReasonCode;
        private int lastAttemptNumber;

        private CapturingStandingOrderRetryPolicyService() {
            super(new StandingOrderModuleConfig());
        }

        @Override
        public RetryDecision evaluateRetry(String retryPolicyCode, String failureReasonCode, int attemptNumber, Instant nowUtc) {
            lastPolicyCode = retryPolicyCode;
            lastFailureReasonCode = failureReasonCode;
            lastAttemptNumber = attemptNumber;
            return nextDecision;
        }
    }

    private static final class CapturingStandingOrderScheduleCalculatorBridge extends StandingOrderScheduleCalculatorBridge {
        private Instant nextExecutionAt;

        private CapturingStandingOrderScheduleCalculatorBridge() {
            super(null);
        }

        @Override
        public Instant nextExecution(StandingOrderEntity standingOrder, Instant currentDueAtUtc) {
            return nextExecutionAt;
        }
    }

    private static final class CapturingBalanceConsistencyService extends BalanceConsistencyService {
        private LockedAccountPair lockedPair;
        private BalanceMutation debitMutation;
        private BalanceMutation creditMutation;
        private ApiErrorException apiException;

        private CapturingBalanceConsistencyService() {
            super(null, null);
        }

        @Override
        public LockedAccountPair lockAccountPair(String sourceAccountId, String destinationAccountId) {
            if (apiException != null) {
                throw apiException;
            }
            return lockedPair;
        }

        @Override
        public BalanceMutation applyCredit(AccountEntity account, BigDecimal amount, String currencyCode) {
            return creditMutation;
        }

        @Override
        public BalanceMutation applyDebit(AccountEntity account, BigDecimal amount, String currencyCode) {
            return debitMutation;
        }
    }

    private static final class InMemoryTransactionRepository implements TransactionRepository {
        private List<TransactionEntity> saveAllResponse;
        private List<TransactionEntity> lastSavedTransactions = List.of();

        @Override
        public TransactionEntity save(TransactionEntity transaction) {
            return transaction;
        }

        @Override
        public List<TransactionEntity> saveAll(List<TransactionEntity> transactions) {
            lastSavedTransactions = new ArrayList<>(transactions);
            if (saveAllResponse != null) {
                return saveAllResponse;
            }
            int counter = 1;
            for (TransactionEntity transaction : transactions) {
                if (transaction.getTransactionId() == null) {
                    transaction.setTransactionId("tx-" + counter);
                    counter++;
                }
            }
            return transactions;
        }

        @Override
        public Optional<TransactionEntity> findById(String transactionId) {
            return Optional.empty();
        }

        @Override
        public List<TransactionEntity> findAccountTransactionsForPeriod(String accountId, Instant periodStartUtc, Instant periodEndUtc) {
            return List.of();
        }

        @Override
        public List<TransactionEntity> findCustomerTransactionsForPeriod(String customerId, Instant periodStartUtc, Instant periodEndUtc) {
            return List.of();
        }

        @Override
        public Page<TransactionEntity> findAccountHistory(
                String accountId,
                Instant startDateUtc,
                Instant endDateUtc,
                TransactionType transactionType,
                Pageable pageable) {
            return Page.empty();
        }

        @Override
        public Page<TransactionEntity> findCustomerHistory(
                String customerId,
                Instant startDateUtc,
                Instant endDateUtc,
                TransactionType transactionType,
                Pageable pageable) {
            return Page.empty();
        }
    }

    private static final class CapturingTransferLinkService extends TransferLinkService {
        private TransferLinkEntity transferToReturn;
        private RuntimeException runtimeException;

        private CapturingTransferLinkService() {
            super(null, null);
        }

        @Override
        public TransferLinkEntity persistTransferLink(
                String debitTransactionId,
                String creditTransactionId,
                String sourceAccountId,
                String destinationAccountId,
                BigDecimal amount,
                String currencyCode) {
            if (runtimeException != null) {
                throw runtimeException;
            }
            return transferToReturn;
        }
    }

    private static final class CapturingStandingOrderLifecycleAuditService extends StandingOrderLifecycleAuditService {
        private final List<LifecycleAuditCall> calls = new ArrayList<>();

        private CapturingStandingOrderLifecycleAuditService() {
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
            calls.add(new LifecycleAuditCall(standingOrderId, eventType, actorUserId, actorRole, reasonCode, metadata));
        }
    }

    private static final class CapturingNotificationAutoTriggerService extends NotificationAutoTriggerService {
        private final List<String> calls = new ArrayList<>();

        private CapturingNotificationAutoTriggerService() {
            super(null);
        }

        @Override
        public void triggerStandingOrderExecuted(
                String sourceAccountId,
                String destinationAccountId,
                String amount,
                String standingOrderId,
                String transferId) {
            calls.add(sourceAccountId + "|" + destinationAccountId + "|" + amount + "|" + standingOrderId + "|" + transferId);
        }
    }

    private static final class SequencedOutcomeOrchestrator extends StandingOrderExecutionOrchestrator {
        private final List<StandingOrderExecutionStatus> statuses;
        private int index;

        private SequencedOutcomeOrchestrator(
                StandingOrderRepository standingOrderRepository,
                List<StandingOrderExecutionStatus> statuses) {
            super(
                    standingOrderRepository,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
            this.statuses = statuses;
        }

        @Override
        public ExecutionOutcome executeStandingOrder(StandingOrderEntity standingOrder) {
            StandingOrderExecutionStatus status = statuses.get(index);
            index++;
            return new ExecutionOutcome(status, "event-" + index);
        }
    }
}

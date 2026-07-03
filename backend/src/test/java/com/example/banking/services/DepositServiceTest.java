package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.transactions.schemas.DepositSchema;
import com.example.banking.api.transactions.schemas.PostingResponseSchema;
import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.lib.security.TransactionAccessPolicy;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.IdempotencyOperationType;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;

class DepositServiceTest {

    private static final String ACCOUNT_ID = "11111111-1111-1111-1111-111111111111";

    private FakeMoneyPolicy moneyPolicy;
    private FakeTransactionAccessPolicy transactionAccessPolicy;
    private FakeBalanceConsistencyService balanceConsistencyService;
    private InMemoryTransactionRepository transactionRepository;
    private FakeMonetaryIdempotencyOrchestrator idempotencyOrchestrator;
    private CapturingTransactionLifecycleAuditService lifecycleAuditService;
    private CapturingNotificationAutoTriggerService notificationAutoTriggerService;
    private DepositService service;

    @BeforeEach
    void setUp() {
        moneyPolicy = new FakeMoneyPolicy();
        transactionAccessPolicy = new FakeTransactionAccessPolicy();
        balanceConsistencyService = new FakeBalanceConsistencyService();
        transactionRepository = new InMemoryTransactionRepository();
        idempotencyOrchestrator = new FakeMonetaryIdempotencyOrchestrator();
        lifecycleAuditService = new CapturingTransactionLifecycleAuditService();
        notificationAutoTriggerService = new CapturingNotificationAutoTriggerService();

        service = new DepositService(
                moneyPolicy,
                transactionAccessPolicy,
                balanceConsistencyService,
                transactionRepository,
                idempotencyOrchestrator,
                lifecycleAuditService,
                notificationAutoTriggerService);

        AccountEntity account = account(ACCOUNT_ID, "AUD");
        transactionAccessPolicy.scopedAccount = account;
        balanceConsistencyService.lockedAccount = account;
        balanceConsistencyService.creditMutation = new BalanceConsistencyService.BalanceMutation(
                account,
                new BigDecimal("100.00"),
                new BigDecimal("110.00"));
    }

    @Test
    void postDepositSuccessCoversMainFlowAndNormalizesRole() {
        PostingResponseSchema response = service.postDeposit(
                new DepositSchema(ACCOUNT_ID, "10.00"),
                "idem-1",
                "actor-1",
                "customer");

        assertNotNull(response);
        assertEquals("txn-1", response.transactionId());
        assertEquals("DEPOSIT", response.transactionType());
        assertEquals("10.00", response.postedAmount());
        assertEquals("AUD", response.currencyCode());
        assertEquals("110.00", response.balanceAfter());

        assertEquals("DEPOSIT|" + ACCOUNT_ID + "|10.00|actor-1", idempotencyOrchestrator.lastHashPayload);
        assertEquals(IdempotencyOperationType.DEPOSIT, idempotencyOrchestrator.lastOperationType);
        assertEquals("idem-1", idempotencyOrchestrator.lastIdempotencyKey);
        assertEquals("hash-1", idempotencyOrchestrator.lastRequestHash);
        assertEquals(1, idempotencyOrchestrator.executeCalls);
        assertEquals(1, idempotencyOrchestrator.supplierExecutionCount);

        assertEquals("10.00", moneyPolicy.lastRawAmount);
        assertEquals("amount", moneyPolicy.lastAmountField);

        assertEquals(2, transactionAccessPolicy.scopeCalls.size());
        assertEquals("actor-1", transactionAccessPolicy.scopeCalls.get(0).actorUserId());
        assertEquals("actor-1", transactionAccessPolicy.scopeCalls.get(1).actorUserId());

        assertEquals(ACCOUNT_ID, balanceConsistencyService.lastLockedAccountId);
        assertEquals("AUD", balanceConsistencyService.lastApplyCurrencyCode);

        assertNotNull(transactionRepository.savedTransaction);
        assertEquals(TransactionType.DEPOSIT, transactionRepository.savedTransaction.getTransactionType());
        assertEquals("CUSTOMER", transactionRepository.savedTransaction.getActorRole());
        assertEquals("actor-1", transactionRepository.savedTransaction.getActorUserId());
        assertNull(transactionRepository.savedTransaction.getCorrelationId());
        assertEquals("{\"operation\":\"DEPOSIT\"}", transactionRepository.savedTransaction.getMetadata());

        assertEquals(1, lifecycleAuditService.successCalls.size());
        assertEquals("DEPOSIT", lifecycleAuditService.successCalls.get(0).eventType());
        assertEquals("CUSTOMER", lifecycleAuditService.successCalls.get(0).actorRole());
        assertEquals(0, lifecycleAuditService.failureCalls.size());

        assertEquals(1, notificationAutoTriggerService.calls.size());
        DepositNotificationCall notificationCall = notificationAutoTriggerService.calls.get(0);
        assertEquals(ACCOUNT_ID, notificationCall.accountId());
        assertEquals("10.00", notificationCall.amount());
        assertEquals("actor-1", notificationCall.actorUserId());
        assertEquals("CUSTOMER", notificationCall.role());
    }

    @Test
    void postDepositSuccessNormalizesBlankRoleToUnknown() {
        PostingResponseSchema response = service.postDeposit(
                new DepositSchema(ACCOUNT_ID, "10.00"),
                "idem-2",
                "actor-2",
                "   ");

        assertEquals("txn-1", response.transactionId());
        assertEquals("UNKNOWN", transactionRepository.savedTransaction.getActorRole());
        assertEquals("UNKNOWN", lifecycleAuditService.successCalls.get(0).actorRole());
        assertEquals("UNKNOWN", notificationAutoTriggerService.calls.get(0).role());
    }

    @Test
    void postDepositSuccessNormalizesNullActorToAnonymous() {
        PostingResponseSchema response = service.postDeposit(
                new DepositSchema(ACCOUNT_ID, "10.00"),
                "idem-3",
                null,
                "ADMIN");

        assertEquals("txn-1", response.transactionId());
        assertEquals("DEPOSIT|" + ACCOUNT_ID + "|10.00|anonymous", idempotencyOrchestrator.lastHashPayload);
        assertEquals("anonymous", transactionAccessPolicy.scopeCalls.get(0).actorUserId());
        assertEquals("anonymous", transactionAccessPolicy.scopeCalls.get(1).actorUserId());
        assertEquals("anonymous", transactionRepository.savedTransaction.getActorUserId());
        assertEquals("anonymous", lifecycleAuditService.successCalls.get(0).actorUserId());
        assertEquals("anonymous", notificationAutoTriggerService.calls.get(0).actorUserId());
    }

    @Test
    void postDepositRecordsFailureWhenIdempotencyExecuteThrowsApiErrorAndNullRole() {
        idempotencyOrchestrator.executeException = new ApiErrorException(
                HttpStatus.CONFLICT,
                "TRANSACTION_IDEMPOTENCY_CONFLICT",
                "Replay mismatch",
                "Idempotency-Key");

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postDeposit(
                        new DepositSchema(ACCOUNT_ID, "10.00"),
                        "idem-4",
                        "actor-4",
                        null));

        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", exception.getCode());
        assertEquals(1, lifecycleAuditService.failureCalls.size());
        FailureCall failure = lifecycleAuditService.failureCalls.get(0);
        assertEquals("DEPOSIT", failure.eventType());
        assertEquals("actor-4", failure.actorUserId());
        assertEquals("UNKNOWN", failure.actorRole());
        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", failure.reasonCode());
        assertEquals("{}", failure.metadata());
        assertEquals(0, idempotencyOrchestrator.supplierExecutionCount);
    }

    @Test
    void postDepositRecordsFailureWhenActorIsBlankAndScopeDenied() {
        transactionAccessPolicy.scopeException = new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "TRANSACTION_FORBIDDEN",
                "Insufficient privileges",
                null);

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postDeposit(
                        new DepositSchema(ACCOUNT_ID, "10.00"),
                        "idem-5",
                        "   ",
                        "admin"));

        assertEquals("TRANSACTION_FORBIDDEN", exception.getCode());
        assertNull(idempotencyOrchestrator.lastHashPayload);
        assertEquals(1, lifecycleAuditService.failureCalls.size());
        FailureCall failure = lifecycleAuditService.failureCalls.get(0);
        assertEquals("anonymous", failure.actorUserId());
        assertEquals("ADMIN", failure.actorRole());
    }

    @Test
    void postDepositRejectsNullAccountId() {
        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postDeposit(
                        new DepositSchema(null, "10.00"),
                        "idem-6",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("accountId", exception.getField());
        assertEquals(0, lifecycleAuditService.failureCalls.size());
    }

    @Test
    void postDepositRejectsBlankAccountId() {
        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postDeposit(
                        new DepositSchema("   ", "10.00"),
                        "idem-7",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("accountId", exception.getField());
        assertEquals(0, lifecycleAuditService.failureCalls.size());
    }

    @Test
    void postDepositRejectsInvalidAccountId() {
        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postDeposit(
                        new DepositSchema("not-a-uuid", "10.00"),
                        "idem-8",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("accountId", exception.getField());
        assertEquals(0, lifecycleAuditService.failureCalls.size());
    }

    @Test
    void postDepositAmountValidationErrorBypassesAuditCatch() {
        moneyPolicy.parseException = new ApiErrorException(
                HttpStatus.BAD_REQUEST,
                "TRANSACTION_VALIDATION_ERROR",
                "amount must be a decimal value",
                "amount");

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postDeposit(
                        new DepositSchema(ACCOUNT_ID, "bad"),
                        "idem-9",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("amount", exception.getField());
        assertEquals(0, lifecycleAuditService.failureCalls.size());
    }

    private static AccountEntity account(String accountId, String currencyCode) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setCurrencyCode(currencyCode);
        account.setStatus("ACTIVE");
        account.setBalance(new BigDecimal("100.00"));
        account.setUpdatedAtUtc(Instant.parse("2026-07-01T00:00:00Z"));
        return account;
    }

    private static final class FakeMoneyPolicy extends MoneyPolicy {
        BigDecimal parsedAmount = new BigDecimal("10.00");
        ApiErrorException parseException;

        String lastRawAmount;
        String lastAmountField;

        FakeMoneyPolicy() {
            super(new TransactionModuleConfig());
        }

        @Override
        public BigDecimal parsePositiveAmount(String rawAmount, String field) {
            lastRawAmount = rawAmount;
            lastAmountField = field;
            if (parseException != null) {
                throw parseException;
            }
            return parsedAmount;
        }
    }

    private static final class FakeTransactionAccessPolicy extends TransactionAccessPolicy {
        final List<ScopeCall> scopeCalls = new ArrayList<>();
        AccountEntity scopedAccount;
        ApiErrorException scopeException;

        FakeTransactionAccessPolicy() {
            super(null, null);
        }

        @Override
        public AccountEntity requireAccountOperationScope(String accountId, String role, String actorUserId, String operation) {
            scopeCalls.add(new ScopeCall(accountId, role, actorUserId, operation));
            if (scopeException != null) {
                throw scopeException;
            }
            return scopedAccount;
        }
    }

    private record ScopeCall(String accountId, String role, String actorUserId, String operation) {
    }

    private static final class FakeBalanceConsistencyService extends BalanceConsistencyService {
        AccountEntity lockedAccount;
        BalanceConsistencyService.BalanceMutation creditMutation;
        String lastLockedAccountId;
        String lastApplyCurrencyCode;

        FakeBalanceConsistencyService() {
            super(null, new FakeMoneyPolicy());
        }

        @Override
        public AccountEntity lockActiveAccount(String accountId) {
            lastLockedAccountId = accountId;
            return lockedAccount;
        }

        @Override
        public BalanceConsistencyService.BalanceMutation applyCredit(AccountEntity account, BigDecimal amount, String currencyCode) {
            lastApplyCurrencyCode = currencyCode;
            return creditMutation;
        }
    }

    private static final class InMemoryTransactionRepository implements TransactionRepository {
        TransactionEntity savedTransaction;

        @Override
        public TransactionEntity save(TransactionEntity transaction) {
            savedTransaction = transaction;
            if (savedTransaction.getTransactionId() == null) {
                savedTransaction.setTransactionId("txn-1");
            }
            return savedTransaction;
        }

        @Override
        public List<TransactionEntity> saveAll(List<TransactionEntity> transactions) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public Optional<TransactionEntity> findById(String transactionId) {
            return Optional.empty();
        }

        @Override
        public List<TransactionEntity> findAccountTransactionsForPeriod(String accountId, Instant periodStartUtc, Instant periodEndUtc) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public List<TransactionEntity> findCustomerTransactionsForPeriod(String customerId, Instant periodStartUtc, Instant periodEndUtc) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public org.springframework.data.domain.Page<TransactionEntity> findAccountHistory(
                String accountId,
                Instant startDateUtc,
                Instant endDateUtc,
                TransactionType transactionType,
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public org.springframework.data.domain.Page<TransactionEntity> findCustomerHistory(
                String customerId,
                Instant startDateUtc,
                Instant endDateUtc,
                TransactionType transactionType,
                org.springframework.data.domain.Pageable pageable) {
            throw new UnsupportedOperationException("Not needed for this test");
        }
    }

    private static final class FakeMonetaryIdempotencyOrchestrator extends MonetaryIdempotencyOrchestrator {
        String lastHashPayload;
        String hashResult = "hash-1";

        ApiErrorException executeException;
        IdempotencyOperationType lastOperationType;
        String lastIdempotencyKey;
        String lastRequestHash;
        int executeCalls;
        int supplierExecutionCount;

        FakeMonetaryIdempotencyOrchestrator() {
            super(null, new ObjectMapper());
        }

        @Override
        public String hashRequest(String canonicalPayload) {
            lastHashPayload = canonicalPayload;
            return hashResult;
        }

        @Override
        public <T> T execute(
                IdempotencyOperationType operationType,
                String idempotencyKey,
                String requestHash,
                java.util.function.Supplier<T> operation,
                Class<T> responseType,
                java.util.function.Function<T, String> responseTransactionIdExtractor) {
            executeCalls++;
            lastOperationType = operationType;
            lastIdempotencyKey = idempotencyKey;
            lastRequestHash = requestHash;

            if (executeException != null) {
                throw executeException;
            }

            supplierExecutionCount++;
            return operation.get();
        }
    }

    private static final class CapturingTransactionLifecycleAuditService extends TransactionLifecycleAuditService {
        final List<SuccessCall> successCalls = new ArrayList<>();
        final List<FailureCall> failureCalls = new ArrayList<>();

        CapturingTransactionLifecycleAuditService() {
            super(null);
        }

        @Override
        public void recordSuccess(String transactionId, String eventType, String actorUserId, String actorRole, String metadata) {
            successCalls.add(new SuccessCall(transactionId, eventType, actorUserId, actorRole, metadata));
        }

        @Override
        public void recordFailure(
                String transactionId,
                String eventType,
                String actorUserId,
                String actorRole,
                String reasonCode,
                String metadata) {
            failureCalls.add(new FailureCall(transactionId, eventType, actorUserId, actorRole, reasonCode, metadata));
        }
    }

    private record SuccessCall(
            String transactionId,
            String eventType,
            String actorUserId,
            String actorRole,
            String metadata) {
    }

    private record FailureCall(
            String transactionId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
    }

    private static final class CapturingNotificationAutoTriggerService extends NotificationAutoTriggerService {
        final List<DepositNotificationCall> calls = new ArrayList<>();

        CapturingNotificationAutoTriggerService() {
            super(null);
        }

        @Override
        public void triggerDepositPosted(String accountId, String amount, String actorUserId, String role) {
            calls.add(new DepositNotificationCall(accountId, amount, actorUserId, role));
        }
    }

    private record DepositNotificationCall(String accountId, String amount, String actorUserId, String role) {
    }
}

package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.transactions.mappers.TransferResponseMapper;
import com.example.banking.api.transactions.schemas.TransferResponseSchema;
import com.example.banking.api.transactions.schemas.TransferSchema;
import com.example.banking.lib.config.TransactionModuleConfig;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.lib.security.TransactionAccessPolicy;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.IdempotencyOperationType;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
import com.example.banking.models.TransferLinkEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

class TransferServiceTest {

    private static final String SOURCE_ACCOUNT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String DESTINATION_ACCOUNT_ID = "22222222-2222-2222-2222-222222222222";

    private FakeMoneyPolicy moneyPolicy;
    private FakeTransactionAccessPolicy transactionAccessPolicy;
    private FakeBalanceConsistencyService balanceConsistencyService;
    private InMemoryTransactionRepository transactionRepository;
    private FakeTransferLinkService transferLinkService;
    private FakeMonetaryIdempotencyOrchestrator idempotencyOrchestrator;
    private CapturingTransactionLifecycleAuditService lifecycleAuditService;
    private FakeTransferResponseMapper transferResponseMapper;
    private CapturingNotificationAutoTriggerService notificationAutoTriggerService;
    private TransferService service;

    @BeforeEach
    void setUp() {
        moneyPolicy = new FakeMoneyPolicy();
        transactionAccessPolicy = new FakeTransactionAccessPolicy();
        balanceConsistencyService = new FakeBalanceConsistencyService();
        transactionRepository = new InMemoryTransactionRepository();
        transferLinkService = new FakeTransferLinkService();
        idempotencyOrchestrator = new FakeMonetaryIdempotencyOrchestrator();
        lifecycleAuditService = new CapturingTransactionLifecycleAuditService();
        transferResponseMapper = new FakeTransferResponseMapper();
        notificationAutoTriggerService = new CapturingNotificationAutoTriggerService();

        service = new TransferService(
                moneyPolicy,
                transactionAccessPolicy,
                balanceConsistencyService,
                transactionRepository,
                transferLinkService,
                idempotencyOrchestrator,
                lifecycleAuditService,
                transferResponseMapper,
                notificationAutoTriggerService);

        AccountEntity source = account(SOURCE_ACCOUNT_ID, "AUD");
        AccountEntity destination = account(DESTINATION_ACCOUNT_ID, "AUD");

        balanceConsistencyService.lockedPair = new BalanceConsistencyService.LockedAccountPair(source, destination);
        balanceConsistencyService.debitMutation = new BalanceConsistencyService.BalanceMutation(
                source,
                new BigDecimal("100.00"),
                new BigDecimal("90.00"));
        balanceConsistencyService.creditMutation = new BalanceConsistencyService.BalanceMutation(
                destination,
                new BigDecimal("50.00"),
                new BigDecimal("60.00"));
    }

    @Test
    void postTransferSuccessCoversMainFlowAndNormalizesRole() {
        TransferResponseSchema response = service.postTransfer(
                new TransferSchema(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, "10.00"),
                "idem-1",
                "actor-1",
                "customer");

        assertNotNull(response);
        assertEquals("transfer-1", response.transferId());
        assertEquals("debit-1", response.debitTransactionId());
        assertEquals("credit-1", response.creditTransactionId());

        assertEquals("TRANSFER|" + SOURCE_ACCOUNT_ID + "|" + DESTINATION_ACCOUNT_ID + "|10.00|actor-1",
                idempotencyOrchestrator.lastHashPayload);
        assertEquals(IdempotencyOperationType.TRANSFER, idempotencyOrchestrator.lastOperationType);
        assertEquals("idem-1", idempotencyOrchestrator.lastIdempotencyKey);
        assertEquals("hash-1", idempotencyOrchestrator.lastRequestHash);
        assertTrue(idempotencyOrchestrator.supplierExecuted);

        assertEquals(4, transactionAccessPolicy.scopeCalls.size());
        assertEquals("actor-1", transactionAccessPolicy.scopeCalls.get(0).actorUserId());
        assertEquals("actor-1", transactionAccessPolicy.scopeCalls.get(3).actorUserId());

        assertEquals("10.00", moneyPolicy.lastRawAmount);
        assertEquals("amount", moneyPolicy.lastAmountField);
        assertEquals("AUD", moneyPolicy.lastEnsureLeftCurrency);
        assertEquals("AUD", moneyPolicy.lastEnsureRightCurrency);

        assertNotNull(transactionRepository.lastSavedTransactions);
        assertEquals(2, transactionRepository.lastSavedTransactions.size());

        TransactionEntity savedDebit = transactionRepository.lastSavedTransactions.stream()
                .filter(candidate -> candidate.getTransactionType() == TransactionType.TRANSFER_DEBIT)
                .findFirst()
                .orElseThrow();
        TransactionEntity savedCredit = transactionRepository.lastSavedTransactions.stream()
                .filter(candidate -> candidate.getTransactionType() == TransactionType.TRANSFER_CREDIT)
                .findFirst()
                .orElseThrow();

        assertEquals("CUSTOMER", savedDebit.getActorRole());
        assertEquals("CUSTOMER", savedCredit.getActorRole());
        assertEquals("{\"operation\":\"TRANSFER_DEBIT\"}", savedDebit.getMetadata());
        assertEquals("{\"operation\":\"TRANSFER_CREDIT\"}", savedCredit.getMetadata());

        assertEquals("debit-1", transferLinkService.lastDebitTransactionId);
        assertEquals("credit-1", transferLinkService.lastCreditTransactionId);
        assertEquals(SOURCE_ACCOUNT_ID, transferLinkService.lastSourceAccountId);
        assertEquals(DESTINATION_ACCOUNT_ID, transferLinkService.lastDestinationAccountId);
        assertEquals("AUD", transferLinkService.lastCurrencyCode);

        assertEquals(2, lifecycleAuditService.successCalls.size());
        assertEquals("TRANSFER_DEBIT", lifecycleAuditService.successCalls.get(0).eventType());
        assertEquals("CUSTOMER", lifecycleAuditService.successCalls.get(0).actorRole());
        assertEquals("TRANSFER_CREDIT", lifecycleAuditService.successCalls.get(1).eventType());
        assertEquals("CUSTOMER", lifecycleAuditService.successCalls.get(1).actorRole());
        assertEquals(0, lifecycleAuditService.failureCalls.size());

        assertEquals(1, notificationAutoTriggerService.calls.size());
        TransferNotificationCall notificationCall = notificationAutoTriggerService.calls.get(0);
        assertEquals(SOURCE_ACCOUNT_ID, notificationCall.sourceAccountId());
        assertEquals(DESTINATION_ACCOUNT_ID, notificationCall.destinationAccountId());
        assertEquals("10.00", notificationCall.amount());
        assertEquals("transfer-1", notificationCall.transferId());
        assertEquals("actor-1", notificationCall.actorUserId());
        assertEquals("CUSTOMER", notificationCall.role());
    }

    @Test
    void postTransferRecordsFailureWhenApiErrorOccursAndNormalizesNullActorAndRole() {
        idempotencyOrchestrator.executeException = new ApiErrorException(
                HttpStatus.CONFLICT,
                "TRANSACTION_IDEMPOTENCY_CONFLICT",
                "Replay mismatch",
                "Idempotency-Key");

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, "10.00"),
                        "idem-2",
                        null,
                        null));

        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", exception.getCode());
        assertEquals("TRANSFER|" + SOURCE_ACCOUNT_ID + "|" + DESTINATION_ACCOUNT_ID + "|10.00|anonymous",
                idempotencyOrchestrator.lastHashPayload);

        assertEquals(1, lifecycleAuditService.failureCalls.size());
        FailureCall failureCall = lifecycleAuditService.failureCalls.get(0);
        assertEquals("TRANSFER", failureCall.eventType());
        assertEquals("anonymous", failureCall.actorUserId());
        assertEquals("UNKNOWN", failureCall.actorRole());
        assertEquals("TRANSACTION_IDEMPOTENCY_CONFLICT", failureCall.reasonCode());
        assertEquals("{}", failureCall.metadata());
    }

    @Test
    void postTransferRecordsFailureWhenActorIsBlank() {
        idempotencyOrchestrator.executeException = new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "TRANSACTION_FORBIDDEN",
                "Forbidden",
                null);

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, "10.00"),
                        "idem-3",
                        "   ",
                        "admin"));

        assertEquals("TRANSACTION_FORBIDDEN", exception.getCode());
        assertEquals("TRANSFER|" + SOURCE_ACCOUNT_ID + "|" + DESTINATION_ACCOUNT_ID + "|10.00|anonymous",
                idempotencyOrchestrator.lastHashPayload);
        assertEquals("anonymous", lifecycleAuditService.failureCalls.get(0).actorUserId());
        assertEquals("ADMIN", lifecycleAuditService.failureCalls.get(0).actorRole());
    }

    @Test
    void postTransferRejectsNullSourceAccountId() {
        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema(null, DESTINATION_ACCOUNT_ID, "10.00"),
                        "idem-4",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("sourceAccountId", exception.getField());
        assertEquals(0, lifecycleAuditService.failureCalls.size());
    }

    @Test
    void postTransferRejectsBlankDestinationAccountId() {
        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema(SOURCE_ACCOUNT_ID, "   ", "10.00"),
                        "idem-5",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("destinationAccountId", exception.getField());
        assertEquals(0, lifecycleAuditService.failureCalls.size());
    }

    @Test
    void postTransferRejectsInvalidUuid() {
        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema("not-a-uuid", DESTINATION_ACCOUNT_ID, "10.00"),
                        "idem-6",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("sourceAccountId", exception.getField());
    }

    @Test
    void postTransferRejectsSameSourceAndDestinationAccount() {
        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema(SOURCE_ACCOUNT_ID, SOURCE_ACCOUNT_ID, "10.00"),
                        "idem-7",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_CONFLICT", exception.getCode());
        assertEquals("destinationAccountId", exception.getField());
    }

    @Test
    void postTransferThrowsWhenDebitTransactionIsMissingAfterSave() {
        transactionRepository.saveMode = SaveMode.ONLY_CREDIT;

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, "10.00"),
                        "idem-8",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_CONFLICT", exception.getCode());
        assertEquals("Transfer debit transaction was not persisted", exception.getMessage());
        assertEquals(1, lifecycleAuditService.failureCalls.size());
    }

    @Test
    void postTransferThrowsWhenCreditTransactionIsMissingAfterSave() {
        transactionRepository.saveMode = SaveMode.ONLY_DEBIT;

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, "10.00"),
                        "idem-9",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_CONFLICT", exception.getCode());
        assertEquals("Transfer credit transaction was not persisted", exception.getMessage());
        assertEquals(1, lifecycleAuditService.failureCalls.size());
    }

    @Test
    void postTransferAmountValidationFailureHappensBeforeAuditCatch() {
        moneyPolicy.parseException = new ApiErrorException(
                HttpStatus.BAD_REQUEST,
                "TRANSACTION_VALIDATION_ERROR",
                "amount must be a decimal value",
                "amount");

        ApiErrorException exception = assertThrows(ApiErrorException.class,
                () -> service.postTransfer(
                        new TransferSchema(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, "bad"),
                        "idem-10",
                        "actor-1",
                        "CUSTOMER"));

        assertEquals("TRANSACTION_VALIDATION_ERROR", exception.getCode());
        assertEquals("amount", exception.getField());
        assertEquals(0, lifecycleAuditService.failureCalls.size());
    }

    @Test
    void postTransferNormalizesBlankRoleToUnknownOnSuccessPath() {
        TransferResponseSchema response = service.postTransfer(
                new TransferSchema(SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, "10.00"),
                "idem-11",
                "actor-2",
                "   ");

        assertEquals("transfer-1", response.transferId());

        TransactionEntity savedDebit = transactionRepository.lastSavedTransactions.stream()
                .filter(candidate -> candidate.getTransactionType() == TransactionType.TRANSFER_DEBIT)
                .findFirst()
                .orElseThrow();
        TransactionEntity savedCredit = transactionRepository.lastSavedTransactions.stream()
                .filter(candidate -> candidate.getTransactionType() == TransactionType.TRANSFER_CREDIT)
                .findFirst()
                .orElseThrow();

        assertEquals("UNKNOWN", savedDebit.getActorRole());
        assertEquals("UNKNOWN", savedCredit.getActorRole());

        assertEquals("UNKNOWN", lifecycleAuditService.successCalls.get(0).actorRole());
        assertEquals("UNKNOWN", lifecycleAuditService.successCalls.get(1).actorRole());
        assertEquals("UNKNOWN", notificationAutoTriggerService.calls.get(0).role());
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

    private enum SaveMode {
        BOTH,
        ONLY_DEBIT,
        ONLY_CREDIT
    }

    private static final class FakeMoneyPolicy extends MoneyPolicy {
        BigDecimal parsedAmount = new BigDecimal("10.00");
        ApiErrorException parseException;
        ApiErrorException ensureSameCurrencyException;

        String lastRawAmount;
        String lastAmountField;
        String lastEnsureLeftCurrency;
        String lastEnsureRightCurrency;
        String lastEnsureField;

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

        @Override
        public void ensureSameCurrency(String leftCurrency, String rightCurrency, String field) {
            lastEnsureLeftCurrency = leftCurrency;
            lastEnsureRightCurrency = rightCurrency;
            lastEnsureField = field;
            if (ensureSameCurrencyException != null) {
                throw ensureSameCurrencyException;
            }
        }
    }

    private static final class FakeTransactionAccessPolicy extends TransactionAccessPolicy {
        final List<ScopeCall> scopeCalls = new ArrayList<>();
        ApiErrorException scopeException;

        FakeTransactionAccessPolicy() {
            super(null, null);
        }

        @Override
        public AccountEntity requireAccountOperationScope(
                String accountId,
                String role,
                String actorUserId,
                String operation) {
            scopeCalls.add(new ScopeCall(accountId, role, actorUserId, operation));
            if (scopeException != null) {
                throw scopeException;
            }
            return null;
        }
    }

    private record ScopeCall(String accountId, String role, String actorUserId, String operation) {
    }

    private static final class FakeBalanceConsistencyService extends BalanceConsistencyService {
        BalanceConsistencyService.LockedAccountPair lockedPair;
        BalanceConsistencyService.BalanceMutation debitMutation;
        BalanceConsistencyService.BalanceMutation creditMutation;

        String lastLockSourceAccountId;
        String lastLockDestinationAccountId;

        FakeBalanceConsistencyService() {
            super(null, new FakeMoneyPolicy());
        }

        @Override
        public LockedAccountPair lockAccountPair(String sourceAccountId, String destinationAccountId) {
            lastLockSourceAccountId = sourceAccountId;
            lastLockDestinationAccountId = destinationAccountId;
            return lockedPair;
        }

        @Override
        public BalanceMutation applyDebit(AccountEntity account, BigDecimal amount, String currencyCode) {
            return debitMutation;
        }

        @Override
        public BalanceMutation applyCredit(AccountEntity account, BigDecimal amount, String currencyCode) {
            return creditMutation;
        }
    }

    private static final class InMemoryTransactionRepository implements TransactionRepository {
        SaveMode saveMode = SaveMode.BOTH;
        List<TransactionEntity> lastSavedTransactions;

        @Override
        public TransactionEntity save(TransactionEntity transaction) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public List<TransactionEntity> saveAll(List<TransactionEntity> transactions) {
            lastSavedTransactions = new ArrayList<>(transactions);

            for (TransactionEntity transaction : lastSavedTransactions) {
                if (transaction.getTransactionType() == TransactionType.TRANSFER_DEBIT) {
                    transaction.setTransactionId("debit-1");
                }
                if (transaction.getTransactionType() == TransactionType.TRANSFER_CREDIT) {
                    transaction.setTransactionId("credit-1");
                }
            }

            if (saveMode == SaveMode.ONLY_DEBIT) {
                return lastSavedTransactions.stream()
                        .filter(candidate -> candidate.getTransactionType() == TransactionType.TRANSFER_DEBIT)
                        .toList();
            }
            if (saveMode == SaveMode.ONLY_CREDIT) {
                return lastSavedTransactions.stream()
                        .filter(candidate -> candidate.getTransactionType() == TransactionType.TRANSFER_CREDIT)
                        .toList();
            }
            return new ArrayList<>(lastSavedTransactions);
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

    private static final class FakeTransferLinkService extends TransferLinkService {
        String lastDebitTransactionId;
        String lastCreditTransactionId;
        String lastSourceAccountId;
        String lastDestinationAccountId;
        BigDecimal lastAmount;
        String lastCurrencyCode;

        FakeTransferLinkService() {
            super(null, new FakeMoneyPolicy());
        }

        @Override
        public TransferLinkEntity persistTransferLink(
                String debitTransactionId,
                String creditTransactionId,
                String sourceAccountId,
                String destinationAccountId,
                BigDecimal amount,
                String currencyCode) {
            lastDebitTransactionId = debitTransactionId;
            lastCreditTransactionId = creditTransactionId;
            lastSourceAccountId = sourceAccountId;
            lastDestinationAccountId = destinationAccountId;
            lastAmount = amount;
            lastCurrencyCode = currencyCode;

            TransferLinkEntity transferLink = new TransferLinkEntity();
            transferLink.setTransferId("transfer-1");
            transferLink.setDebitTransactionId(debitTransactionId);
            transferLink.setCreditTransactionId(creditTransactionId);
            transferLink.setSourceAccountId(sourceAccountId);
            transferLink.setDestinationAccountId(destinationAccountId);
            transferLink.setAmount(amount);
            transferLink.setCurrencyCode(currencyCode);
            return transferLink;
        }
    }

    private static final class FakeMonetaryIdempotencyOrchestrator extends MonetaryIdempotencyOrchestrator {
        String lastHashPayload;
        String hashResponse = "hash-1";

        ApiErrorException executeException;
        IdempotencyOperationType lastOperationType;
        String lastIdempotencyKey;
        String lastRequestHash;
        boolean supplierExecuted;

        FakeMonetaryIdempotencyOrchestrator() {
            super(null, new ObjectMapper());
        }

        @Override
        public String hashRequest(String canonicalPayload) {
            lastHashPayload = canonicalPayload;
            return hashResponse;
        }

        @Override
        public <T> T execute(
                IdempotencyOperationType operationType,
                String idempotencyKey,
                String requestHash,
                java.util.function.Supplier<T> operation,
                Class<T> responseType,
                java.util.function.Function<T, String> responseTransactionIdExtractor) {
            lastOperationType = operationType;
            lastIdempotencyKey = idempotencyKey;
            lastRequestHash = requestHash;

            if (executeException != null) {
                throw executeException;
            }

            supplierExecuted = true;
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

    private static final class FakeTransferResponseMapper extends TransferResponseMapper {
        @Override
        public TransferResponseSchema toSchema(
                TransferLinkEntity transferLink,
                TransactionEntity debitTransaction,
                TransactionEntity creditTransaction) {
            return new TransferResponseSchema(
                    transferLink.getTransferId(),
                    debitTransaction.getTransactionId(),
                    creditTransaction.getTransactionId(),
                    debitTransaction.getAmount().toPlainString(),
                    debitTransaction.getCurrencyCode(),
                    debitTransaction.getBalanceAfter().toPlainString(),
                    creditTransaction.getBalanceAfter().toPlainString(),
                    debitTransaction.getPostedAtUtc().toString());
        }
    }

    private static final class CapturingNotificationAutoTriggerService extends NotificationAutoTriggerService {
        final List<TransferNotificationCall> calls = new ArrayList<>();

        CapturingNotificationAutoTriggerService() {
            super(null);
        }

        @Override
        public void triggerTransferCompleted(
                String sourceAccountId,
                String destinationAccountId,
                String amount,
                String transferId,
                String actorUserId,
                String role) {
            calls.add(new TransferNotificationCall(
                    sourceAccountId,
                    destinationAccountId,
                    amount,
                    transferId,
                    actorUserId,
                    role));
        }
    }

    private record TransferNotificationCall(
            String sourceAccountId,
            String destinationAccountId,
            String amount,
            String transferId,
            String actorUserId,
            String role) {
    }
}

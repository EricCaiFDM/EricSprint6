package com.example.banking.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.observability.StandingOrderLogFields;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderExecutionEventEntity;
import com.example.banking.models.StandingOrderExecutionStatus;
import com.example.banking.models.StandingOrderLifecycleState;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
import com.example.banking.models.TransferLinkEntity;

@Service
public class StandingOrderExecutionOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(StandingOrderExecutionOrchestrator.class);

    private final StandingOrderRepository standingOrderRepository;
    private final StandingOrderExecutionEventRepository executionEventRepository;
    private final StandingOrderRetryPolicyService retryPolicyService;
    private final StandingOrderScheduleCalculatorBridge scheduleCalculatorBridge;
    private final BalanceConsistencyService balanceConsistencyService;
    private final TransactionRepository transactionRepository;
    private final TransferLinkService transferLinkService;
    private final StandingOrderLifecycleAuditService lifecycleAuditService;

    public StandingOrderExecutionOrchestrator(
            StandingOrderRepository standingOrderRepository,
            StandingOrderExecutionEventRepository executionEventRepository,
            StandingOrderRetryPolicyService retryPolicyService,
            StandingOrderScheduleCalculatorBridge scheduleCalculatorBridge,
            BalanceConsistencyService balanceConsistencyService,
            TransactionRepository transactionRepository,
            TransferLinkService transferLinkService,
            StandingOrderLifecycleAuditService lifecycleAuditService) {
        this.standingOrderRepository = standingOrderRepository;
        this.executionEventRepository = executionEventRepository;
        this.retryPolicyService = retryPolicyService;
        this.scheduleCalculatorBridge = scheduleCalculatorBridge;
        this.balanceConsistencyService = balanceConsistencyService;
        this.transactionRepository = transactionRepository;
        this.transferLinkService = transferLinkService;
        this.lifecycleAuditService = lifecycleAuditService;
    }

    @Transactional
    public ExecutionSummary processWindow(Instant windowStartUtc, Instant windowEndUtc) {
        List<StandingOrderEntity> dueOrders = standingOrderRepository.findDueWithinWindow(windowStartUtc, windowEndUtc);

        int processed = 0;
        int succeeded = 0;
        int failed = 0;
        int retried = 0;

        for (StandingOrderEntity standingOrder : dueOrders) {
            processed++;
            ExecutionOutcome outcome = executeStandingOrder(standingOrder);
            if (outcome.status() == StandingOrderExecutionStatus.SUCCEEDED) {
                succeeded++;
            } else if (outcome.status() == StandingOrderExecutionStatus.RETRY_SCHEDULED) {
                retried++;
            } else {
                failed++;
            }
        }

        return new ExecutionSummary(processed, succeeded, failed, retried);
    }

    @Transactional
    public ExecutionOutcome executeStandingOrder(StandingOrderEntity standingOrder) {
        Instant startedAtUtc = Instant.now();
        Instant dueAtUtc = standingOrder.getNextExecutionAtUtc() == null ? startedAtUtc : standingOrder.getNextExecutionAtUtc();
        int attemptNumber = (int) executionEventRepository.countByStandingOrderId(standingOrder.getStandingOrderId()) + 1;

        try {
            TransferLinkEntity transfer = performTransfer(standingOrder, dueAtUtc);

            Instant nextExecutionAtUtc = scheduleCalculatorBridge.nextExecution(standingOrder, dueAtUtc);
            if (nextExecutionAtUtc == null) {
                standingOrder.setLifecycleState(StandingOrderLifecycleState.COMPLETED);
                lifecycleAuditService.recordEvent(
                        standingOrder.getStandingOrderId(),
                        "COMPLETED",
                        "system-scheduler",
                        "SYSTEM",
                        null,
                        "{}");
            }
            standingOrder.setNextExecutionAtUtc(nextExecutionAtUtc);
            standingOrder.setUpdatedAtUtc(Instant.now());
            standingOrderRepository.save(standingOrder);

            StandingOrderExecutionEventEntity event = saveExecutionEvent(
                    standingOrder,
                    dueAtUtc,
                    startedAtUtc,
                    Instant.now(),
                    StandingOrderExecutionStatus.SUCCEEDED,
                    attemptNumber,
                    transfer.getTransferId(),
                    null,
                    null,
                    "{}");

            logOutcome(standingOrder.getStandingOrderId(), event, StandingOrderExecutionStatus.SUCCEEDED);
            return new ExecutionOutcome(StandingOrderExecutionStatus.SUCCEEDED, event.getExecutionEventId());
        } catch (ApiErrorException apiErrorException) {
            return handleFailure(standingOrder, dueAtUtc, startedAtUtc, attemptNumber, apiErrorException.getCode(), apiErrorException.getMessage());
        } catch (Exception exception) {
            return handleFailure(standingOrder, dueAtUtc, startedAtUtc, attemptNumber, "FAILED_DEPENDENCY_OUTAGE", exception.getMessage());
        }
    }

    private ExecutionOutcome handleFailure(
            StandingOrderEntity standingOrder,
            Instant dueAtUtc,
            Instant startedAtUtc,
            int attemptNumber,
            String reasonCode,
            String message) {
        StandingOrderExecutionStatus mappedStatus = mapFailureStatus(reasonCode);
        StandingOrderRetryPolicyService.RetryDecision retryDecision = retryPolicyService.evaluateRetry(
                standingOrder.getRetryPolicyCode(),
                mappedStatus.name(),
                attemptNumber,
                Instant.now());

        StandingOrderExecutionStatus storedStatus = mappedStatus;
        Instant nextRetryAtUtc = null;
        if (retryDecision.scheduleRetry()) {
            storedStatus = StandingOrderExecutionStatus.RETRY_SCHEDULED;
            nextRetryAtUtc = retryDecision.nextRetryAtUtc();
            standingOrder.setNextExecutionAtUtc(nextRetryAtUtc);
        } else {
            Instant nextExecutionAtUtc = scheduleCalculatorBridge.nextExecution(standingOrder, dueAtUtc);
            standingOrder.setNextExecutionAtUtc(nextExecutionAtUtc);
            if (nextExecutionAtUtc == null) {
                standingOrder.setLifecycleState(StandingOrderLifecycleState.COMPLETED);
                lifecycleAuditService.recordEvent(
                        standingOrder.getStandingOrderId(),
                        "COMPLETED",
                        "system-scheduler",
                        "SYSTEM",
                        reasonCode,
                        "{}");
            }
        }

        standingOrder.setUpdatedAtUtc(Instant.now());
        standingOrderRepository.save(standingOrder);

        StandingOrderExecutionEventEntity event = saveExecutionEvent(
                standingOrder,
                dueAtUtc,
                startedAtUtc,
                Instant.now(),
                storedStatus,
                attemptNumber,
                null,
                nextRetryAtUtc,
                reasonCode,
                "{\"message\":\"" + sanitize(message) + "\"}");

        logOutcome(standingOrder.getStandingOrderId(), event, storedStatus);
        return new ExecutionOutcome(storedStatus, event.getExecutionEventId());
    }

    private void logOutcome(String standingOrderId, StandingOrderExecutionEventEntity event, StandingOrderExecutionStatus status) {
        logger.info(
                "Standing order execution outcome {}={} {}={} {}={} {}={} {}={} {}={}",
                StandingOrderLogFields.STANDING_ORDER_ID,
                standingOrderId,
                StandingOrderLogFields.EXECUTION_EVENT_ID,
                event.getExecutionEventId(),
                StandingOrderLogFields.STATUS,
                status.name(),
                StandingOrderLogFields.ATTEMPT_NUMBER,
                event.getAttemptNumber(),
                StandingOrderLogFields.REASON_CODE,
                event.getReasonCode(),
                StandingOrderLogFields.TRANSFER_REFERENCE_ID,
                event.getTransferReferenceId());
    }

    private String sanitize(String message) {
        if (message == null) {
            return "unknown";
        }
        return message.replace('"', '\'').replace('\n', ' ').replace('\r', ' ');
    }

    private StandingOrderExecutionStatus mapFailureStatus(String reasonCode) {
        if ("TRANSACTION_INSUFFICIENT_FUNDS".equals(reasonCode)) {
            return StandingOrderExecutionStatus.FAILED_INSUFFICIENT_FUNDS;
        }
        if ("TRANSACTION_ACCOUNT_NOT_FOUND".equals(reasonCode)
                || "STANDING_ORDER_ACCOUNT_NOT_FOUND".equals(reasonCode)
                || "TRANSACTION_CONFLICT".equals(reasonCode)) {
            return StandingOrderExecutionStatus.FAILED_INELIGIBLE_ACCOUNT;
        }
        return StandingOrderExecutionStatus.FAILED_DEPENDENCY_OUTAGE;
    }

    private StandingOrderExecutionEventEntity saveExecutionEvent(
            StandingOrderEntity standingOrder,
            Instant dueAtUtc,
            Instant startedAtUtc,
            Instant completedAtUtc,
            StandingOrderExecutionStatus status,
            int attemptNumber,
            String transferReferenceId,
            Instant nextRetryAtUtc,
            String reasonCode,
            String metadata) {
        StandingOrderExecutionEventEntity event = new StandingOrderExecutionEventEntity();
        event.setExecutionEventId(UUID.randomUUID().toString());
        event.setStandingOrderId(standingOrder.getStandingOrderId());
        event.setDueAtUtc(dueAtUtc);
        event.setStartedAtUtc(startedAtUtc);
        event.setCompletedAtUtc(completedAtUtc);
        event.setStatus(status);
        event.setAttemptNumber(attemptNumber);
        event.setTransferReferenceId(transferReferenceId);
        event.setNextRetryAtUtc(nextRetryAtUtc);
        event.setReasonCode(reasonCode);
        event.setMetadata(metadata == null ? "{}" : metadata);
        return executionEventRepository.save(event);
    }

    private TransferLinkEntity performTransfer(StandingOrderEntity standingOrder, Instant dueAtUtc) {
        BalanceConsistencyService.LockedAccountPair lockedAccounts = balanceConsistencyService.lockAccountPair(
                standingOrder.getSourceAccountId(),
                standingOrder.getDestinationAccountId());

        AccountEntity source = lockedAccounts.sourceAccount();
        AccountEntity destination = lockedAccounts.destinationAccount();

        BalanceConsistencyService.BalanceMutation debitMutation = balanceConsistencyService.applyDebit(
                source,
                standingOrder.getAmount(),
                source.getCurrencyCode());

        BalanceConsistencyService.BalanceMutation creditMutation = balanceConsistencyService.applyCredit(
                destination,
                standingOrder.getAmount(),
                destination.getCurrencyCode());

        String correlationId = UUID.randomUUID().toString();
        String idempotencyKeyBase = "standing-order-" + standingOrder.getStandingOrderId().substring(0, 8);
        String idempotencyKey = idempotencyKeyBase + "-" + Math.abs(dueAtUtc.toEpochMilli());

        TransactionEntity debitTransaction = new TransactionEntity();
        debitTransaction.setAccountId(source.getAccountId());
        debitTransaction.setTransactionType(TransactionType.TRANSFER_DEBIT);
        debitTransaction.setAmount(standingOrder.getAmount());
        debitTransaction.setCurrencyCode(source.getCurrencyCode());
        debitTransaction.setPostedAtUtc(Instant.now());
        debitTransaction.setIdempotencyKey(idempotencyKey + "-d");
        debitTransaction.setCorrelationId(correlationId);
        debitTransaction.setActorUserId("system-scheduler");
        debitTransaction.setActorRole("SYSTEM");
        debitTransaction.setBalanceBefore(debitMutation.balanceBefore());
        debitTransaction.setBalanceAfter(debitMutation.balanceAfter());
        debitTransaction.setMetadata("{\"operation\":\"STANDING_ORDER_DEBIT\",\"standingOrderId\":\""
                + standingOrder.getStandingOrderId() + "\"}");
        debitTransaction.setCreatedAtUtc(Instant.now());

        TransactionEntity creditTransaction = new TransactionEntity();
        creditTransaction.setAccountId(destination.getAccountId());
        creditTransaction.setTransactionType(TransactionType.TRANSFER_CREDIT);
        creditTransaction.setAmount(standingOrder.getAmount());
        creditTransaction.setCurrencyCode(destination.getCurrencyCode());
        creditTransaction.setPostedAtUtc(Instant.now());
        creditTransaction.setIdempotencyKey(idempotencyKey + "-c");
        creditTransaction.setCorrelationId(correlationId);
        creditTransaction.setActorUserId("system-scheduler");
        creditTransaction.setActorRole("SYSTEM");
        creditTransaction.setBalanceBefore(creditMutation.balanceBefore());
        creditTransaction.setBalanceAfter(creditMutation.balanceAfter());
        creditTransaction.setMetadata("{\"operation\":\"STANDING_ORDER_CREDIT\",\"standingOrderId\":\""
                + standingOrder.getStandingOrderId() + "\"}");
        creditTransaction.setCreatedAtUtc(Instant.now());

        List<TransactionEntity> savedTransactions = transactionRepository.saveAll(List.of(debitTransaction, creditTransaction));
        TransactionEntity savedDebit = savedTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.TRANSFER_DEBIT)
                .findFirst()
                .orElseThrow();
        TransactionEntity savedCredit = savedTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.TRANSFER_CREDIT)
                .findFirst()
                .orElseThrow();

        return transferLinkService.persistTransferLink(
                savedDebit.getTransactionId(),
                savedCredit.getTransactionId(),
                source.getAccountId(),
                destination.getAccountId(),
                standingOrder.getAmount(),
                source.getCurrencyCode());
    }

    public record ExecutionOutcome(StandingOrderExecutionStatus status, String executionEventId) {
    }

    public record ExecutionSummary(int processed, int succeeded, int failed, int retried) {
    }
}

package com.example.banking.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.transactions.mappers.TransferResponseMapper;
import com.example.banking.api.transactions.schemas.TransferResponseSchema;
import com.example.banking.api.transactions.schemas.TransferSchema;
import com.example.banking.lib.errors.TransactionErrors;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.lib.security.TransactionAccessPolicy;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.IdempotencyOperationType;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;
import com.example.banking.models.TransferLinkEntity;

@Service
public class TransferService {
    private final MoneyPolicy moneyPolicy;
    private final TransactionAccessPolicy transactionAccessPolicy;
    private final BalanceConsistencyService balanceConsistencyService;
    private final TransactionRepository transactionRepository;
    private final TransferLinkService transferLinkService;
    private final MonetaryIdempotencyOrchestrator idempotencyOrchestrator;
    private final TransactionLifecycleAuditService lifecycleAuditService;
    private final TransferResponseMapper transferResponseMapper;
    private final NotificationAutoTriggerService notificationAutoTriggerService;

    public TransferService(
            MoneyPolicy moneyPolicy,
            TransactionAccessPolicy transactionAccessPolicy,
            BalanceConsistencyService balanceConsistencyService,
            TransactionRepository transactionRepository,
            TransferLinkService transferLinkService,
            MonetaryIdempotencyOrchestrator idempotencyOrchestrator,
            TransactionLifecycleAuditService lifecycleAuditService,
            TransferResponseMapper transferResponseMapper,
            NotificationAutoTriggerService notificationAutoTriggerService) {
        this.moneyPolicy = moneyPolicy;
        this.transactionAccessPolicy = transactionAccessPolicy;
        this.balanceConsistencyService = balanceConsistencyService;
        this.transactionRepository = transactionRepository;
        this.transferLinkService = transferLinkService;
        this.idempotencyOrchestrator = idempotencyOrchestrator;
        this.lifecycleAuditService = lifecycleAuditService;
        this.transferResponseMapper = transferResponseMapper;
        this.notificationAutoTriggerService = notificationAutoTriggerService;
    }

    @Transactional
    public TransferResponseSchema postTransfer(
            TransferSchema request,
            String idempotencyKey,
            String actorUserId,
            String role) {
        String actorId = normalizeActor(actorUserId);
        String sourceAccountId = normalizeUuid(request.sourceAccountId(), "sourceAccountId");
        String destinationAccountId = normalizeUuid(request.destinationAccountId(), "destinationAccountId");
        if (sourceAccountId.equals(destinationAccountId)) {
            throw TransactionErrors.conflict(
                    "sourceAccountId and destinationAccountId must be different",
                    "destinationAccountId");
        }

        BigDecimal amount = moneyPolicy.parsePositiveAmount(request.amount(), "amount");

        try {
            transactionAccessPolicy.requireAccountOperationScope(sourceAccountId, role, actorId, "transfer");
            transactionAccessPolicy.requireAccountOperationScope(destinationAccountId, role, actorId, "transfer");

            String requestHash = idempotencyOrchestrator.hashRequest(
                    "TRANSFER|" + sourceAccountId + "|" + destinationAccountId + "|" + amount.toPlainString() + "|" + actorId);

            return idempotencyOrchestrator.execute(
                    IdempotencyOperationType.TRANSFER,
                    idempotencyKey,
                    requestHash,
                    () -> executeTransfer(sourceAccountId, destinationAccountId, amount, idempotencyKey, actorId, role),
                    TransferResponseSchema.class,
                    TransferResponseSchema::transferId);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(
                    null,
                    "TRANSFER",
                    actorId,
                    normalizeRole(role),
                    exception.getCode(),
                    "{}");
            throw exception;
        }
    }

    private TransferResponseSchema executeTransfer(
            String sourceAccountId,
            String destinationAccountId,
            BigDecimal amount,
            String idempotencyKey,
            String actorUserId,
            String role) {
        BalanceConsistencyService.LockedAccountPair lockedPair = balanceConsistencyService.lockAccountPair(
                sourceAccountId,
                destinationAccountId);
        AccountEntity source = lockedPair.sourceAccount();
        AccountEntity destination = lockedPair.destinationAccount();

        transactionAccessPolicy.requireAccountOperationScope(source.getAccountId(), role, actorUserId, "transfer");
        transactionAccessPolicy.requireAccountOperationScope(destination.getAccountId(), role, actorUserId, "transfer");

        moneyPolicy.ensureSameCurrency(source.getCurrencyCode(), destination.getCurrencyCode(), "currencyCode");

        BalanceConsistencyService.BalanceMutation debitMutation = balanceConsistencyService.applyDebit(
                source,
                amount,
                source.getCurrencyCode());
        BalanceConsistencyService.BalanceMutation creditMutation = balanceConsistencyService.applyCredit(
                destination,
                amount,
                destination.getCurrencyCode());

        Instant postedAt = Instant.now();
        String correlationId = UUID.randomUUID().toString();

        TransactionEntity debitTransaction = new TransactionEntity();
        debitTransaction.setAccountId(source.getAccountId());
        debitTransaction.setTransactionType(TransactionType.TRANSFER_DEBIT);
        debitTransaction.setAmount(amount);
        debitTransaction.setCurrencyCode(source.getCurrencyCode());
        debitTransaction.setPostedAtUtc(postedAt);
        debitTransaction.setIdempotencyKey(idempotencyKey);
        debitTransaction.setCorrelationId(correlationId);
        debitTransaction.setActorUserId(actorUserId);
        debitTransaction.setActorRole(normalizeRole(role));
        debitTransaction.setBalanceBefore(debitMutation.balanceBefore());
        debitTransaction.setBalanceAfter(debitMutation.balanceAfter());
        debitTransaction.setMetadata("{\"operation\":\"TRANSFER_DEBIT\"}");
        debitTransaction.setCreatedAtUtc(postedAt);

        TransactionEntity creditTransaction = new TransactionEntity();
        creditTransaction.setAccountId(destination.getAccountId());
        creditTransaction.setTransactionType(TransactionType.TRANSFER_CREDIT);
        creditTransaction.setAmount(amount);
        creditTransaction.setCurrencyCode(destination.getCurrencyCode());
        creditTransaction.setPostedAtUtc(postedAt);
        creditTransaction.setIdempotencyKey(idempotencyKey);
        creditTransaction.setCorrelationId(correlationId);
        creditTransaction.setActorUserId(actorUserId);
        creditTransaction.setActorRole(normalizeRole(role));
        creditTransaction.setBalanceBefore(creditMutation.balanceBefore());
        creditTransaction.setBalanceAfter(creditMutation.balanceAfter());
        creditTransaction.setMetadata("{\"operation\":\"TRANSFER_CREDIT\"}");
        creditTransaction.setCreatedAtUtc(postedAt);

        List<TransactionEntity> savedTransactions = transactionRepository.saveAll(List.of(debitTransaction, creditTransaction));
        TransactionEntity savedDebit = savedTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.TRANSFER_DEBIT)
                .findFirst()
                .orElseThrow(() -> TransactionErrors.conflict("Transfer debit transaction was not persisted", null));
        TransactionEntity savedCredit = savedTransactions.stream()
                .filter(transaction -> transaction.getTransactionType() == TransactionType.TRANSFER_CREDIT)
                .findFirst()
                .orElseThrow(() -> TransactionErrors.conflict("Transfer credit transaction was not persisted", null));

        TransferLinkEntity transferLink = transferLinkService.persistTransferLink(
                savedDebit.getTransactionId(),
                savedCredit.getTransactionId(),
                source.getAccountId(),
                destination.getAccountId(),
                amount,
                source.getCurrencyCode());

        lifecycleAuditService.recordSuccess(
                savedDebit.getTransactionId(),
                "TRANSFER_DEBIT",
                actorUserId,
                normalizeRole(role),
                "{\"sourceAccountId\":\"" + source.getAccountId() + "\",\"destinationAccountId\":\""
                        + destination.getAccountId() + "\"}");
        lifecycleAuditService.recordSuccess(
                savedCredit.getTransactionId(),
                "TRANSFER_CREDIT",
                actorUserId,
                normalizeRole(role),
                "{\"sourceAccountId\":\"" + source.getAccountId() + "\",\"destinationAccountId\":\""
                        + destination.getAccountId() + "\"}");

        notificationAutoTriggerService.triggerTransferCompleted(
            source.getAccountId(),
            destination.getAccountId(),
            amount.toPlainString(),
            transferLink.getTransferId(),
            actorUserId,
            normalizeRole(role));

        return transferResponseMapper.toSchema(transferLink, savedDebit, savedCredit);
    }

    private String normalizeUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw TransactionErrors.validation(field + " is required", field);
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw TransactionErrors.validation(field + " must be a UUID", field);
        }
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "UNKNOWN";
        }
        return role.toUpperCase(Locale.ROOT);
    }
}

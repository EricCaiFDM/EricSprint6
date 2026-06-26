package com.example.banking.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.transactions.schemas.PostingResponseSchema;
import com.example.banking.api.transactions.schemas.WithdrawalSchema;
import com.example.banking.lib.errors.TransactionErrors;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.lib.security.TransactionAccessPolicy;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.IdempotencyOperationType;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;

@Service
public class WithdrawalService {
    private final MoneyPolicy moneyPolicy;
    private final TransactionAccessPolicy transactionAccessPolicy;
    private final BalanceConsistencyService balanceConsistencyService;
    private final TransactionRepository transactionRepository;
    private final MonetaryIdempotencyOrchestrator idempotencyOrchestrator;
    private final TransactionLifecycleAuditService lifecycleAuditService;

    public WithdrawalService(
            MoneyPolicy moneyPolicy,
            TransactionAccessPolicy transactionAccessPolicy,
            BalanceConsistencyService balanceConsistencyService,
            TransactionRepository transactionRepository,
            MonetaryIdempotencyOrchestrator idempotencyOrchestrator,
            TransactionLifecycleAuditService lifecycleAuditService) {
        this.moneyPolicy = moneyPolicy;
        this.transactionAccessPolicy = transactionAccessPolicy;
        this.balanceConsistencyService = balanceConsistencyService;
        this.transactionRepository = transactionRepository;
        this.idempotencyOrchestrator = idempotencyOrchestrator;
        this.lifecycleAuditService = lifecycleAuditService;
    }

    @Transactional
    public PostingResponseSchema postWithdrawal(
            WithdrawalSchema request,
            String idempotencyKey,
            String actorUserId,
            String role) {
        String actorId = normalizeActor(actorUserId);
        String accountId = normalizeUuid(request.accountId(), "accountId");
        BigDecimal amount = moneyPolicy.parsePositiveAmount(request.amount(), "amount");

        try {
            AccountEntity scopedAccount = transactionAccessPolicy.requireAccountOperationScope(
                    accountId,
                    role,
                    actorId,
                    "withdraw");

            String requestHash = idempotencyOrchestrator.hashRequest(
                    "WITHDRAWAL|" + scopedAccount.getAccountId() + "|" + amount.toPlainString() + "|" + actorId);

            return idempotencyOrchestrator.execute(
                    IdempotencyOperationType.WITHDRAWAL,
                    idempotencyKey,
                    requestHash,
                    () -> executeWithdrawal(scopedAccount, amount, idempotencyKey, actorId, role),
                    PostingResponseSchema.class,
                    PostingResponseSchema::transactionId);
        } catch (ApiErrorException exception) {
            lifecycleAuditService.recordFailure(
                    null,
                    "WITHDRAWAL",
                    actorId,
                    normalizeRole(role),
                    exception.getCode(),
                    "{}");
            throw exception;
        }
    }

    private PostingResponseSchema executeWithdrawal(
            AccountEntity scopedAccount,
            BigDecimal amount,
            String idempotencyKey,
            String actorUserId,
            String role) {
        AccountEntity lockedAccount = balanceConsistencyService.lockActiveAccount(scopedAccount.getAccountId());
        transactionAccessPolicy.requireAccountOperationScope(lockedAccount.getAccountId(), role, actorUserId, "withdraw");

        BalanceConsistencyService.BalanceMutation mutation = balanceConsistencyService.applyDebit(
                lockedAccount,
                amount,
                lockedAccount.getCurrencyCode());

        TransactionEntity transaction = new TransactionEntity();
        transaction.setAccountId(lockedAccount.getAccountId());
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setAmount(amount);
        transaction.setCurrencyCode(lockedAccount.getCurrencyCode());
        transaction.setPostedAtUtc(Instant.now());
        transaction.setIdempotencyKey(idempotencyKey);
        transaction.setCorrelationId(null);
        transaction.setActorUserId(actorUserId);
        transaction.setActorRole(normalizeRole(role));
        transaction.setBalanceBefore(mutation.balanceBefore());
        transaction.setBalanceAfter(mutation.balanceAfter());
        transaction.setMetadata("{\"operation\":\"WITHDRAWAL\"}");
        transaction.setCreatedAtUtc(Instant.now());

        TransactionEntity saved = transactionRepository.save(transaction);
        lifecycleAuditService.recordSuccess(
                saved.getTransactionId(),
                "WITHDRAWAL",
                actorUserId,
                normalizeRole(role),
                "{\"accountId\":\"" + lockedAccount.getAccountId() + "\"}");

        return new PostingResponseSchema(
                saved.getTransactionId(),
                saved.getTransactionType().name(),
                saved.getAmount().toPlainString(),
                saved.getCurrencyCode(),
                saved.getBalanceAfter().toPlainString(),
                saved.getPostedAtUtc().toString());
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

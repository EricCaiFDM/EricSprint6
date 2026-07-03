package com.example.banking.services.statement;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.banking.lib.TransactionJpaRepository;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;

@Service
public class StatementComputationService {
    private final TransactionJpaRepository transactionJpaRepository;

    public StatementComputationService(TransactionJpaRepository transactionJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
    }

    public ComputationResult compute(String accountId, java.time.Instant periodStartUtc, java.time.Instant periodEndUtc) {
        List<TransactionEntity> beforePeriod = transactionJpaRepository
                .findByAccountIdAndPostedAtUtcLessThanOrderByPostedAtUtcAsc(accountId, periodStartUtc);

        List<TransactionEntity> periodTransactions = transactionJpaRepository
                .findByAccountIdAndPostedAtUtcGreaterThanEqualAndPostedAtUtcLessThanOrderByPostedAtUtcAsc(
                        accountId,
                        periodStartUtc,
                        periodEndUtc);

        BigDecimal openingBalance = beforePeriod.isEmpty()
                ? BigDecimal.ZERO
                : beforePeriod.get(beforePeriod.size() - 1).getBalanceAfter();

        BigDecimal closingBalance = periodTransactions.isEmpty()
                ? openingBalance
                : periodTransactions.get(periodTransactions.size() - 1).getBalanceAfter();

        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;

        for (TransactionEntity transaction : periodTransactions) {
            if (transaction.getTransactionType() == TransactionType.DEPOSIT
                    || transaction.getTransactionType() == TransactionType.TRANSFER_CREDIT) {
                creditTotal = creditTotal.add(transaction.getAmount());
            } else {
                debitTotal = debitTotal.add(transaction.getAmount());
            }
        }

        return new ComputationResult(
                openingBalance,
                closingBalance,
                debitTotal,
                creditTotal,
                periodTransactions.size());
    }

    public record ComputationResult(
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal debitTotal,
            BigDecimal creditTotal,
            int transactionCount) {
    }
}

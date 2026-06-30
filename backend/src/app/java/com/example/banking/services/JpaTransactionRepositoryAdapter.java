package com.example.banking.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.banking.lib.TransactionJpaRepository;
import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;

@Repository
public class JpaTransactionRepositoryAdapter implements TransactionRepository {
    private final TransactionJpaRepository transactionJpaRepository;

    public JpaTransactionRepositoryAdapter(TransactionJpaRepository transactionJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
    }

    @Override
    public TransactionEntity save(TransactionEntity transaction) {
        return transactionJpaRepository.save(transaction);
    }

    @Override
    public List<TransactionEntity> saveAll(List<TransactionEntity> transactions) {
        return transactionJpaRepository.saveAll(transactions);
    }

    @Override
    public Optional<TransactionEntity> findById(String transactionId) {
        return transactionJpaRepository.findByTransactionId(transactionId);
    }

    @Override
    public List<TransactionEntity> findAccountTransactionsForPeriod(
            String accountId,
            Instant periodStartUtc,
            Instant periodEndUtc) {
        return transactionJpaRepository
                .findByAccountIdAndPostedAtUtcGreaterThanEqualAndPostedAtUtcLessThanOrderByPostedAtUtcAsc(
                        accountId,
                        periodStartUtc,
                        periodEndUtc);
    }

                @Override
                public List<TransactionEntity> findCustomerTransactionsForPeriod(
                    String customerId,
                    Instant periodStartUtc,
                    Instant periodEndUtc) {
                return transactionJpaRepository.findCustomerTransactionsForPeriod(
                    customerId,
                    periodStartUtc,
                    periodEndUtc);
                }

    @Override
    public Page<TransactionEntity> findAccountHistory(
            String accountId,
            Instant startDateUtc,
            Instant endDateUtc,
            TransactionType transactionType,
            Pageable pageable) {
        return transactionJpaRepository.findAccountHistory(accountId, startDateUtc, endDateUtc, transactionType, pageable);
    }

    @Override
    public Page<TransactionEntity> findCustomerHistory(
            String customerId,
            Instant startDateUtc,
            Instant endDateUtc,
            TransactionType transactionType,
            Pageable pageable) {
        return transactionJpaRepository.findCustomerHistory(
                customerId,
                startDateUtc,
                endDateUtc,
                transactionType,
                pageable);
    }
}

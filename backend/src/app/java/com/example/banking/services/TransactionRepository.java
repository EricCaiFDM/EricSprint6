package com.example.banking.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;

public interface TransactionRepository {
    TransactionEntity save(TransactionEntity transaction);

    List<TransactionEntity> saveAll(List<TransactionEntity> transactions);

    Optional<TransactionEntity> findById(String transactionId);

    List<TransactionEntity> findAccountTransactionsForPeriod(
            String accountId,
            Instant periodStartUtc,
            Instant periodEndUtc);

    Page<TransactionEntity> findAccountHistory(
            String accountId,
            Instant startDateUtc,
            Instant endDateUtc,
            TransactionType transactionType,
            Pageable pageable);

    Page<TransactionEntity> findCustomerHistory(
            String customerId,
            Instant startDateUtc,
            Instant endDateUtc,
            TransactionType transactionType,
            Pageable pageable);
}

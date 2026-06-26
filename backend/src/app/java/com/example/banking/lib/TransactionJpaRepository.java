package com.example.banking.lib;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.banking.models.TransactionEntity;
import com.example.banking.models.TransactionType;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, String> {
    Optional<TransactionEntity> findByTransactionId(String transactionId);

    @Query("""
            SELECT t
            FROM TransactionEntity t
            WHERE t.accountId = :accountId
              AND (:startDateUtc IS NULL OR t.postedAtUtc >= :startDateUtc)
              AND (:endDateUtc IS NULL OR t.postedAtUtc <= :endDateUtc)
              AND (:transactionType IS NULL OR t.transactionType = :transactionType)
            """)
    Page<TransactionEntity> findAccountHistory(
            @Param("accountId") String accountId,
            @Param("startDateUtc") Instant startDateUtc,
            @Param("endDateUtc") Instant endDateUtc,
            @Param("transactionType") TransactionType transactionType,
            Pageable pageable);

    @Query("""
            SELECT t
            FROM TransactionEntity t
            WHERE t.accountId IN (
                    SELECT a.accountId
                    FROM AccountEntity a
                    WHERE a.customerId = :customerId
                      AND a.deletedAt IS NULL)
              AND (:startDateUtc IS NULL OR t.postedAtUtc >= :startDateUtc)
              AND (:endDateUtc IS NULL OR t.postedAtUtc <= :endDateUtc)
              AND (:transactionType IS NULL OR t.transactionType = :transactionType)
            """)
    Page<TransactionEntity> findCustomerHistory(
            @Param("customerId") String customerId,
            @Param("startDateUtc") Instant startDateUtc,
            @Param("endDateUtc") Instant endDateUtc,
            @Param("transactionType") TransactionType transactionType,
            Pageable pageable);
}

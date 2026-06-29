package com.example.banking.lib;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.banking.models.statement.MonthlyStatement;

public interface MonthlyStatementJpaRepository extends JpaRepository<MonthlyStatement, String> {
    Optional<MonthlyStatement> findByStatementId(String statementId);

    List<MonthlyStatement> findByAccountIdAndPeriodYearMonthOrderByArtifactVersionDesc(String accountId, String periodYearMonth);

    @Query("""
            SELECT s
            FROM MonthlyStatement s
            WHERE (:accountId IS NULL OR s.accountId = :accountId)
              AND (:periodYearMonth IS NULL OR s.periodYearMonth = :periodYearMonth)
              AND (
                    UPPER(:role) = 'ADMIN'
                    OR s.accountId IN (
                        SELECT a.accountId
                        FROM AccountEntity a
                        WHERE a.deletedAt IS NULL
                          AND (
                                a.ownerUserId = :actorUserId
                                OR a.createdByUserId = :actorUserId
                                OR a.customerId IN (
                                    SELECT c.customerId
                                    FROM CustomerEntity c
                                    WHERE c.deletedAt IS NULL
                                      AND (c.ownerUserId = :actorUserId OR c.createdByUserId = :actorUserId)
                                )
                              )
                    )
                  )
            """)
    Page<MonthlyStatement> listByScope(
            @Param("actorUserId") String actorUserId,
            @Param("role") String role,
            @Param("accountId") String accountId,
            @Param("periodYearMonth") String periodYearMonth,
            Pageable pageable);
}

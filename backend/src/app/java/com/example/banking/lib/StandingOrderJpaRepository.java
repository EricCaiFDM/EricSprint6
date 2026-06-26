package com.example.banking.lib;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleState;

public interface StandingOrderJpaRepository extends JpaRepository<StandingOrderEntity, String> {
    Optional<StandingOrderEntity> findByStandingOrderId(String standingOrderId);

    @Query("""
            SELECT so
            FROM StandingOrderEntity so
            WHERE so.lifecycleState = :lifecycleState
              AND so.nextExecutionAtUtc IS NOT NULL
              AND so.nextExecutionAtUtc >= :windowStartUtc
              AND so.nextExecutionAtUtc <= :windowEndUtc
            ORDER BY so.nextExecutionAtUtc ASC
            """)
    List<StandingOrderEntity> findDueWithinWindow(
            @Param("lifecycleState") StandingOrderLifecycleState lifecycleState,
            @Param("windowStartUtc") Instant windowStartUtc,
            @Param("windowEndUtc") Instant windowEndUtc);

    @Query("""
            SELECT so
            FROM StandingOrderEntity so
            WHERE (:role = 'ADMIN')
               OR so.sourceAccountId IN (
                    SELECT a.accountId
                    FROM AccountEntity a
                    WHERE a.customerId IN (
                        SELECT c.customerId
                        FROM CustomerEntity c
                        WHERE c.ownerUserId = :actorUserId
                           OR c.createdByUserId = :actorUserId)
               )
            ORDER BY so.updatedAtUtc DESC
            """)
    Page<StandingOrderEntity> listByScope(
            @Param("actorUserId") String actorUserId,
            @Param("role") String role,
            Pageable pageable);
}

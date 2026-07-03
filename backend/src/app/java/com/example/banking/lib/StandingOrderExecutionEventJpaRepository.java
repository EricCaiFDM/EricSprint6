package com.example.banking.lib;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.banking.models.StandingOrderExecutionEventEntity;

public interface StandingOrderExecutionEventJpaRepository extends JpaRepository<StandingOrderExecutionEventEntity, String> {
    Page<StandingOrderExecutionEventEntity> findByStandingOrderIdOrderByStartedAtUtcDesc(
            String standingOrderId,
            Pageable pageable);

    long countByStandingOrderId(String standingOrderId);
}

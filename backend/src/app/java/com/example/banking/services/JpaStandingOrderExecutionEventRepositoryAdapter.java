package com.example.banking.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.example.banking.lib.StandingOrderExecutionEventJpaRepository;
import com.example.banking.models.StandingOrderExecutionEventEntity;

@Repository
public class JpaStandingOrderExecutionEventRepositoryAdapter implements StandingOrderExecutionEventRepository {
    private final StandingOrderExecutionEventJpaRepository executionEventJpaRepository;

    public JpaStandingOrderExecutionEventRepositoryAdapter(StandingOrderExecutionEventJpaRepository executionEventJpaRepository) {
        this.executionEventJpaRepository = executionEventJpaRepository;
    }

    @Override
    public StandingOrderExecutionEventEntity save(StandingOrderExecutionEventEntity event) {
        return executionEventJpaRepository.save(event);
    }

    @Override
    public long countByStandingOrderId(String standingOrderId) {
        return executionEventJpaRepository.countByStandingOrderId(standingOrderId);
    }

    @Override
    public Page<StandingOrderExecutionEventEntity> listByStandingOrderId(String standingOrderId, int page, int pageSize) {
        int normalizedPage = Math.max(0, page - 1);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        return executionEventJpaRepository.findByStandingOrderIdOrderByStartedAtUtcDesc(
                standingOrderId,
                PageRequest.of(normalizedPage, normalizedPageSize));
    }
}

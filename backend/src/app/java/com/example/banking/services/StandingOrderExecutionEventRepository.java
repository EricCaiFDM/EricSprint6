package com.example.banking.services;

import org.springframework.data.domain.Page;

import com.example.banking.models.StandingOrderExecutionEventEntity;

public interface StandingOrderExecutionEventRepository {
    StandingOrderExecutionEventEntity save(StandingOrderExecutionEventEntity event);

    long countByStandingOrderId(String standingOrderId);

    Page<StandingOrderExecutionEventEntity> listByStandingOrderId(String standingOrderId, int page, int pageSize);
}

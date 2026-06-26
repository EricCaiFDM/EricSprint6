package com.example.banking.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.example.banking.models.StandingOrderEntity;

public interface StandingOrderRepository {
    StandingOrderEntity save(StandingOrderEntity standingOrder);

    Optional<StandingOrderEntity> findById(String standingOrderId);

    List<StandingOrderEntity> findDueWithinWindow(Instant windowStartUtc, Instant windowEndUtc);

    Page<StandingOrderEntity> listByScope(String actorUserId, String role, int page, int pageSize);
}

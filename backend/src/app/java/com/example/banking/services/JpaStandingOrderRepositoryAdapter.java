package com.example.banking.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import com.example.banking.lib.StandingOrderJpaRepository;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleState;

@Repository
public class JpaStandingOrderRepositoryAdapter implements StandingOrderRepository {
    private final StandingOrderJpaRepository standingOrderJpaRepository;

    public JpaStandingOrderRepositoryAdapter(StandingOrderJpaRepository standingOrderJpaRepository) {
        this.standingOrderJpaRepository = standingOrderJpaRepository;
    }

    @Override
    public StandingOrderEntity save(StandingOrderEntity standingOrder) {
        return standingOrderJpaRepository.save(standingOrder);
    }

    @Override
    public Optional<StandingOrderEntity> findById(String standingOrderId) {
        return standingOrderJpaRepository.findByStandingOrderId(standingOrderId);
    }

    @Override
    public List<StandingOrderEntity> findDueWithinWindow(Instant windowStartUtc, Instant windowEndUtc) {
        return standingOrderJpaRepository.findDueWithinWindow(
                StandingOrderLifecycleState.ACTIVE,
                windowStartUtc,
                windowEndUtc);
    }

    @Override
    public Page<StandingOrderEntity> listByScope(String actorUserId, String role, int page, int pageSize) {
        int normalizedPage = Math.max(0, page - 1);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, 100));
        return standingOrderJpaRepository.listByScope(
                actorUserId,
                role,
                PageRequest.of(normalizedPage, normalizedPageSize, Sort.by(Sort.Direction.DESC, "updatedAtUtc")));
    }
}

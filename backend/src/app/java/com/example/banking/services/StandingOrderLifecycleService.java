package com.example.banking.services;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.lib.errors.StandingOrderErrors;
import com.example.banking.lib.scheduling.StandingOrderScheduleCalculator;
import com.example.banking.lib.security.StandingOrderAccessPolicy;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleState;

@Service
public class StandingOrderLifecycleService {
    private final StandingOrderRepository standingOrderRepository;
    private final StandingOrderAccessPolicy accessPolicy;
    private final StandingOrderLifecyclePolicyService lifecyclePolicyService;
    private final StandingOrderLifecycleAuditService auditService;
    private final StandingOrderScheduleCalculator scheduleCalculator;

    public StandingOrderLifecycleService(
            StandingOrderRepository standingOrderRepository,
            StandingOrderAccessPolicy accessPolicy,
            StandingOrderLifecyclePolicyService lifecyclePolicyService,
            StandingOrderLifecycleAuditService auditService,
            StandingOrderScheduleCalculator scheduleCalculator) {
        this.standingOrderRepository = standingOrderRepository;
        this.accessPolicy = accessPolicy;
        this.lifecyclePolicyService = lifecyclePolicyService;
        this.auditService = auditService;
        this.scheduleCalculator = scheduleCalculator;
    }

    public StandingOrderEntity pause(String standingOrderId, String actorUserId, String role) {
        return transition(standingOrderId, actorUserId, role, "PAUSED", lifecyclePolicyService::pause);
    }

    public StandingOrderEntity resume(String standingOrderId, String actorUserId, String role) {
        return transition(standingOrderId, actorUserId, role, "RESUMED", lifecyclePolicyService::resume);
    }

    public StandingOrderEntity cancel(String standingOrderId, String actorUserId, String role) {
        return transition(standingOrderId, actorUserId, role, "CANCELLED", lifecyclePolicyService::cancel);
    }

    private StandingOrderEntity transition(
            String standingOrderId,
            String actorUserId,
            String role,
            String eventType,
            TransitionOperator operator) {
        String normalizedStandingOrderId = normalizeUuid(standingOrderId, "standingOrderId");
        String actorId = normalizeActor(actorUserId);

        StandingOrderEntity standingOrder = standingOrderRepository.findById(normalizedStandingOrderId)
                .orElseThrow(() -> StandingOrderErrors.notFound(normalizedStandingOrderId));

        accessPolicy.requireStandingOrderScope(standingOrder, role, actorId, "update");

        StandingOrderLifecycleState nextState = operator.apply(standingOrder.getLifecycleState());
        standingOrder.setLifecycleState(nextState);

        if (nextState == StandingOrderLifecycleState.ACTIVE) {
            standingOrder.setNextExecutionAtUtc(scheduleCalculator.calculateInitialNextExecutionAt(
                    standingOrder.getEffectiveFromUtc(),
                    standingOrder.getEffectiveToUtc(),
                    standingOrder.getCadence(),
                    Instant.now()));
        } else if (nextState == StandingOrderLifecycleState.PAUSED || nextState == StandingOrderLifecycleState.CANCELLED) {
            standingOrder.setNextExecutionAtUtc(null);
        }

        standingOrder.setUpdatedAtUtc(Instant.now());
        StandingOrderEntity saved = standingOrderRepository.save(standingOrder);

        auditService.recordEvent(saved.getStandingOrderId(), eventType, actorId, role, null, "{}");

        return saved;
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId;
    }

    private String normalizeUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw StandingOrderErrors.validation(field + " is required", field);
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw StandingOrderErrors.validation(field + " must be a UUID", field);
        }
    }

    @FunctionalInterface
    private interface TransitionOperator {
        StandingOrderLifecycleState apply(StandingOrderLifecycleState state);
    }
}

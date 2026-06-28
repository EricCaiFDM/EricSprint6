package com.example.banking.services;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.api.standingorders.schemas.UpdateStandingOrderSchema;
import com.example.banking.lib.config.StandingOrderModuleConfig;
import com.example.banking.lib.errors.StandingOrderErrors;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.lib.scheduling.StandingOrderScheduleCalculator;
import com.example.banking.lib.security.StandingOrderAccessPolicy;
import com.example.banking.models.StandingOrderCadence;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleState;

@Service
public class UpdateStandingOrderService {
    private final StandingOrderRepository standingOrderRepository;
    private final StandingOrderAccessPolicy accessPolicy;
    private final StandingOrderLifecyclePolicyService lifecyclePolicyService;
    private final StandingOrderScheduleCalculator scheduleCalculator;
    private final StandingOrderLifecycleAuditService auditService;
    private final MoneyPolicy moneyPolicy;
    private final StandingOrderModuleConfig standingOrderModuleConfig;

    public UpdateStandingOrderService(
            StandingOrderRepository standingOrderRepository,
            StandingOrderAccessPolicy accessPolicy,
            StandingOrderLifecyclePolicyService lifecyclePolicyService,
            StandingOrderScheduleCalculator scheduleCalculator,
            StandingOrderLifecycleAuditService auditService,
            MoneyPolicy moneyPolicy,
            StandingOrderModuleConfig standingOrderModuleConfig) {
        this.standingOrderRepository = standingOrderRepository;
        this.accessPolicy = accessPolicy;
        this.lifecyclePolicyService = lifecyclePolicyService;
        this.scheduleCalculator = scheduleCalculator;
        this.auditService = auditService;
        this.moneyPolicy = moneyPolicy;
        this.standingOrderModuleConfig = standingOrderModuleConfig;
    }

    public StandingOrderEntity update(
            String standingOrderId,
            UpdateStandingOrderSchema request,
            String actorUserId,
            String role) {
        String normalizedStandingOrderId = normalizeUuid(standingOrderId, "standingOrderId");
        String actorId = normalizeActor(actorUserId);

        StandingOrderEntity standingOrder = standingOrderRepository.findById(normalizedStandingOrderId)
                .orElseThrow(() -> StandingOrderErrors.notFound(normalizedStandingOrderId));

        accessPolicy.requireStandingOrderScope(standingOrder, role, actorId, "update");
        lifecyclePolicyService.enforceUpdatableState(standingOrder.getLifecycleState());

        boolean hasAnyField = request.amount() != null
                || request.cadence() != null
                || request.effectiveFromUtc() != null
                || request.effectiveToUtc() != null
                || request.retryPolicyCode() != null;

        if (!hasAnyField) {
            throw StandingOrderErrors.validation("At least one mutable field is required", null);
        }

        if (request.amount() != null) {
            standingOrder.setAmount(moneyPolicy.parsePositiveAmount(request.amount(), "amount"));
        }

        if (request.cadence() != null) {
            standingOrder.setCadence(parseCadence(request.cadence()));
        }

        Instant effectiveFromUtc = request.effectiveFromUtc() == null
                ? standingOrder.getEffectiveFromUtc()
                : parseInstant(request.effectiveFromUtc(), "effectiveFromUtc");

        Instant effectiveToUtc;
        if (request.effectiveToUtc() == null) {
            effectiveToUtc = standingOrder.getEffectiveToUtc();
        } else if (request.effectiveToUtc().isBlank()) {
            effectiveToUtc = null;
        } else {
            effectiveToUtc = parseInstant(request.effectiveToUtc(), "effectiveToUtc");
        }

        scheduleCalculator.validateEffectiveWindow(effectiveFromUtc, effectiveToUtc);

        standingOrder.setEffectiveFromUtc(effectiveFromUtc);
        standingOrder.setEffectiveToUtc(effectiveToUtc);

        if (request.retryPolicyCode() != null) {
            standingOrder.setRetryPolicyCode(normalizeRetryPolicyCode(request.retryPolicyCode()));
        }

        if (standingOrder.getLifecycleState() == StandingOrderLifecycleState.ACTIVE) {
            Instant recomputedNextExecution = scheduleCalculator.calculateInitialNextExecutionAt(
                    standingOrder.getEffectiveFromUtc(),
                    standingOrder.getEffectiveToUtc(),
                    standingOrder.getCadence(),
                    Instant.now());
            standingOrder.setNextExecutionAtUtc(recomputedNextExecution);
            if (recomputedNextExecution == null) {
                standingOrder.setLifecycleState(StandingOrderLifecycleState.COMPLETED);
            }
        }

        standingOrder.setUpdatedAtUtc(Instant.now());
        StandingOrderEntity saved = standingOrderRepository.save(standingOrder);

        auditService.recordEvent(
                saved.getStandingOrderId(),
                "UPDATED",
                actorId,
                role,
                null,
                "{\"amount\":\"" + saved.getAmount().toPlainString() + "\"}");

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

    private Instant parseInstant(String value, String field) {
        if (value == null || value.isBlank()) {
            throw StandingOrderErrors.validation(field + " is required", field);
        }
        try {
            return Instant.parse(value.trim());
        } catch (Exception exception) {
            throw StandingOrderErrors.validation(field + " must be an ISO-8601 UTC datetime", field);
        }
    }

    private StandingOrderCadence parseCadence(String cadence) {
        try {
            return StandingOrderCadence.valueOf(cadence.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw StandingOrderErrors.validation("cadence must be DAILY, WEEKLY, or MONTHLY", "cadence");
        }
    }

    private String normalizeRetryPolicyCode(String retryPolicyCode) {
        if (retryPolicyCode == null || retryPolicyCode.isBlank()) {
            return standingOrderModuleConfig.getDefaultRetryPolicyCode();
        }
        return retryPolicyCode.trim().toUpperCase(Locale.ROOT);
    }
}

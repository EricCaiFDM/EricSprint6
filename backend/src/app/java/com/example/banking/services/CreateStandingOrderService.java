package com.example.banking.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.api.standingorders.schemas.CreateStandingOrderSchema;
import com.example.banking.lib.config.StandingOrderModuleConfig;
import com.example.banking.lib.errors.StandingOrderErrors;
import com.example.banking.lib.finance.MoneyPolicy;
import com.example.banking.lib.scheduling.StandingOrderScheduleCalculator;
import com.example.banking.lib.security.StandingOrderAccessPolicy;
import com.example.banking.models.AccountEntity;
import com.example.banking.models.StandingOrderCadence;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderLifecycleState;

@Service
public class CreateStandingOrderService {
    private final MoneyPolicy moneyPolicy;
    private final StandingOrderAccessPolicy accessPolicy;
    private final StandingOrderScheduleCalculator scheduleCalculator;
    private final StandingOrderRepository standingOrderRepository;
    private final StandingOrderLifecycleAuditService auditService;
    private final StandingOrderModuleConfig standingOrderModuleConfig;

    public CreateStandingOrderService(
            MoneyPolicy moneyPolicy,
            StandingOrderAccessPolicy accessPolicy,
            StandingOrderScheduleCalculator scheduleCalculator,
            StandingOrderRepository standingOrderRepository,
            StandingOrderLifecycleAuditService auditService,
            StandingOrderModuleConfig standingOrderModuleConfig) {
        this.moneyPolicy = moneyPolicy;
        this.accessPolicy = accessPolicy;
        this.scheduleCalculator = scheduleCalculator;
        this.standingOrderRepository = standingOrderRepository;
        this.auditService = auditService;
        this.standingOrderModuleConfig = standingOrderModuleConfig;
    }

    public StandingOrderEntity create(CreateStandingOrderSchema request, String actorUserId, String role) {
        String actorId = normalizeActor(actorUserId);

        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw StandingOrderErrors.validation(
                    "sourceAccountId and destinationAccountId must be different",
                    "destinationAccountId");
        }

        AccountEntity sourceAccount = accessPolicy.requireAccountScope(
                normalizeUuid(request.sourceAccountId(), "sourceAccountId"),
                role,
                actorId,
                "sourceAccountId");
        AccountEntity destinationAccount = accessPolicy.requireAccountScope(
                normalizeUuid(request.destinationAccountId(), "destinationAccountId"),
                role,
                actorId,
                "destinationAccountId");

        ensureActive(sourceAccount, "sourceAccountId");
        ensureActive(destinationAccount, "destinationAccountId");
        moneyPolicy.ensureSameCurrency(sourceAccount.getCurrencyCode(), destinationAccount.getCurrencyCode(), "currencyCode");

        BigDecimal amount = moneyPolicy.parsePositiveAmount(request.amount(), "amount");
        StandingOrderCadence cadence = parseCadence(request.cadence());

        Instant effectiveFromUtc = parseInstant(request.effectiveFromUtc(), "effectiveFromUtc");
        Instant effectiveToUtc = parseOptionalInstant(request.effectiveToUtc(), "effectiveToUtc");

        Instant nextExecutionAtUtc = scheduleCalculator.calculateInitialNextExecutionAt(
                effectiveFromUtc,
                effectiveToUtc,
                cadence,
                Instant.now());

        if (nextExecutionAtUtc == null) {
            throw StandingOrderErrors.validation(
                    "No executable schedule remains within the effective date range",
                    "effectiveToUtc");
        }

        StandingOrderEntity standingOrder = new StandingOrderEntity();
        standingOrder.setStandingOrderId(UUID.randomUUID().toString());
        standingOrder.setSourceAccountId(sourceAccount.getAccountId());
        standingOrder.setDestinationAccountId(destinationAccount.getAccountId());
        standingOrder.setAmount(amount);
        standingOrder.setCurrencyCode(sourceAccount.getCurrencyCode());
        standingOrder.setCadence(cadence);
        standingOrder.setScheduleConfig("{\"timezone\":\"UTC\"}");
        standingOrder.setEffectiveFromUtc(effectiveFromUtc);
        standingOrder.setEffectiveToUtc(effectiveToUtc);
        standingOrder.setNextExecutionAtUtc(nextExecutionAtUtc);
        standingOrder.setLifecycleState(StandingOrderLifecycleState.ACTIVE);
        standingOrder.setRetryPolicyCode(normalizeRetryPolicyCode(request.retryPolicyCode()));
        standingOrder.setCreatedByUserId(actorId);
        standingOrder.setUpdatedAtUtc(Instant.now());

        StandingOrderEntity saved = standingOrderRepository.save(standingOrder);
        auditService.recordEvent(
                saved.getStandingOrderId(),
                "CREATED",
                actorId,
                role,
                null,
                "{\"sourceAccountId\":\"" + saved.getSourceAccountId() + "\",\"destinationAccountId\":\""
                        + saved.getDestinationAccountId() + "\"}");

        return saved;
    }

    private void ensureActive(AccountEntity account, String field) {
        if (account.getStatus() == null || !"ACTIVE".equalsIgnoreCase(account.getStatus())) {
            throw StandingOrderErrors.validation("Account must be ACTIVE", field);
        }
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

    private StandingOrderCadence parseCadence(String cadence) {
        if (cadence == null || cadence.isBlank()) {
            throw StandingOrderErrors.validation("cadence is required", "cadence");
        }
        try {
            return StandingOrderCadence.valueOf(cadence.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw StandingOrderErrors.validation("cadence must be DAILY, WEEKLY, or MONTHLY", "cadence");
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

    private Instant parseOptionalInstant(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (Exception exception) {
            throw StandingOrderErrors.validation(field + " must be an ISO-8601 UTC datetime", field);
        }
    }

    private String normalizeRetryPolicyCode(String retryPolicyCode) {
        if (retryPolicyCode == null || retryPolicyCode.isBlank()) {
            return standingOrderModuleConfig.getDefaultRetryPolicyCode();
        }
        return retryPolicyCode.trim().toUpperCase(Locale.ROOT);
    }
}

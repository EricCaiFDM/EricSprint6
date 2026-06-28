package com.example.banking.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.banking.lib.config.StandingOrderModuleConfig;
import com.example.banking.lib.errors.StandingOrderErrors;
import com.example.banking.lib.security.StandingOrderAccessPolicy;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderExecutionEventEntity;

@Service
public class ListStandingOrderExecutionsService {
    private final StandingOrderRepository standingOrderRepository;
    private final StandingOrderExecutionEventRepository executionEventRepository;
    private final StandingOrderAccessPolicy accessPolicy;
    private final StandingOrderModuleConfig config;

    public ListStandingOrderExecutionsService(
            StandingOrderRepository standingOrderRepository,
            StandingOrderExecutionEventRepository executionEventRepository,
            StandingOrderAccessPolicy accessPolicy,
            StandingOrderModuleConfig config) {
        this.standingOrderRepository = standingOrderRepository;
        this.executionEventRepository = executionEventRepository;
        this.accessPolicy = accessPolicy;
        this.config = config;
    }

    public Page<StandingOrderExecutionEventEntity> listExecutions(
            String standingOrderId,
            int page,
            int pageSize,
            String actorUserId,
            String role) {
        String normalizedId = normalizeUuid(standingOrderId, "standingOrderId");
        String actorId = normalizeActor(actorUserId);

        StandingOrderEntity standingOrder = standingOrderRepository.findById(normalizedId)
                .orElseThrow(() -> StandingOrderErrors.notFound(normalizedId));

        accessPolicy.requireStandingOrderScope(standingOrder, role, actorId, "read");

        int normalizedPageSize = Math.max(1, Math.min(pageSize, config.getMaxPageSize()));
        int normalizedPage = Math.max(1, page);

        return executionEventRepository.listByStandingOrderId(normalizedId, normalizedPage, normalizedPageSize);
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

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId;
    }
}

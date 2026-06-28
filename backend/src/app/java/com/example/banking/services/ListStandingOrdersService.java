package com.example.banking.services;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.banking.lib.config.StandingOrderModuleConfig;
import com.example.banking.lib.security.StandingOrderAccessPolicy;
import com.example.banking.models.StandingOrderEntity;

@Service
public class ListStandingOrdersService {
    private final StandingOrderRepository standingOrderRepository;
    private final StandingOrderAccessPolicy accessPolicy;
    private final StandingOrderModuleConfig config;

    public ListStandingOrdersService(
            StandingOrderRepository standingOrderRepository,
            StandingOrderAccessPolicy accessPolicy,
            StandingOrderModuleConfig config) {
        this.standingOrderRepository = standingOrderRepository;
        this.accessPolicy = accessPolicy;
        this.config = config;
    }

    public Page<StandingOrderEntity> listByScope(String actorUserId, String role, int page, int pageSize) {
        accessPolicy.enforceManageAccess(role);
        int normalizedPage = Math.max(page, 1);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, config.getMaxPageSize()));
        return standingOrderRepository.listByScope(actorUserId, role, normalizedPage, normalizedPageSize);
    }
}

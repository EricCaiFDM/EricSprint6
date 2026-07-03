package com.example.banking.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.banking.lib.config.NotificationModuleConfig;
import com.example.banking.lib.errors.NotificationErrors;
import com.example.banking.lib.security.NotificationAccessPolicy;
import com.example.banking.models.NotificationDispatchAttemptEntity;
import com.example.banking.models.NotificationEventEntity;

@Service
public class ListNotificationAttemptsService {
    private final NotificationEventRepository eventRepository;
    private final NotificationDispatchAttemptRepository attemptRepository;
    private final NotificationAccessPolicy accessPolicy;
    private final NotificationModuleConfig config;

    public ListNotificationAttemptsService(
            NotificationEventRepository eventRepository,
            NotificationDispatchAttemptRepository attemptRepository,
            NotificationAccessPolicy accessPolicy,
            NotificationModuleConfig config) {
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.accessPolicy = accessPolicy;
        this.config = config;
    }

    public Page<NotificationDispatchAttemptEntity> list(
            String notificationEventId,
            int page,
            int pageSize,
            String actorUserId,
            String role) {
        String normalizedId = normalizeUuid(notificationEventId, "notificationEventId");
        NotificationEventEntity event = eventRepository.findById(normalizedId)
                .orElseThrow(() -> NotificationErrors.notFound(normalizedId));

        accessPolicy.requireEventScope(event, role, normalizeActor(actorUserId), "read");

        int normalizedPage = Math.max(1, page);
        int normalizedPageSize = Math.max(1, Math.min(pageSize, config.getMaxPageSize()));
        return attemptRepository.listByEventId(normalizedId, normalizedPage, normalizedPageSize);
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId.trim();
    }

    private String normalizeUuid(String value, String field) {
        if (value == null || value.isBlank()) {
            throw NotificationErrors.validation(field + " is required", field);
        }
        try {
            return UUID.fromString(value.trim()).toString();
        } catch (IllegalArgumentException exception) {
            throw NotificationErrors.validation(field + " must be a UUID", field);
        }
    }
}

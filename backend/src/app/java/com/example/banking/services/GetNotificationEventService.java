package com.example.banking.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.lib.errors.NotificationErrors;
import com.example.banking.lib.security.NotificationAccessPolicy;
import com.example.banking.models.NotificationDeliveryOutcomeEntity;
import com.example.banking.models.NotificationEvent;
import com.example.banking.models.NotificationEventEntity;

@Service
public class GetNotificationEventService {
    private final NotificationEventRepository eventRepository;
    private final NotificationDeliveryOutcomeRepository outcomeRepository;
    private final NotificationAccessPolicy accessPolicy;

    public GetNotificationEventService(
            NotificationEventRepository eventRepository,
            NotificationDeliveryOutcomeRepository outcomeRepository,
            NotificationAccessPolicy accessPolicy) {
        this.eventRepository = eventRepository;
        this.outcomeRepository = outcomeRepository;
        this.accessPolicy = accessPolicy;
    }

    public NotificationEvent getById(String notificationEventId, String actorUserId, String role) {
        String normalizedId = normalizeUuid(notificationEventId, "notificationEventId");
        NotificationEventEntity event = eventRepository.findById(normalizedId)
                .orElseThrow(() -> NotificationErrors.notFound(normalizedId));

        accessPolicy.requireEventScope(event, role, normalizeActor(actorUserId), "read");

        NotificationDeliveryOutcomeEntity outcome = outcomeRepository.findByEventId(normalizedId).orElse(null);
        return new NotificationEvent(
                event.getNotificationEventId(),
                event.getEventType(),
                event.getRecipientScopeType(),
                event.getRecipientScopeId(),
                event.getStatus(),
                event.getTriggeredAtUtc(),
                event.getCompletedAtUtc(),
                outcome == null ? null : outcome.getFinalStatus(),
                outcome == null ? null : outcome.getReasonCode());
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

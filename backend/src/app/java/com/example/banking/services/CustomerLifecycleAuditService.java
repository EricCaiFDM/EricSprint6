package com.example.banking.services;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.CustomerLifecycleEventJpaRepository;
import com.example.banking.models.CustomerLifecycleEventEntity;

@Service
public class CustomerLifecycleAuditService {
    private final CustomerLifecycleEventJpaRepository eventRepository;

    public CustomerLifecycleAuditService(CustomerLifecycleEventJpaRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void recordSuccess(String customerId, String eventType, String actorUserId, String actorRole) {
        saveEvent(customerId, eventType, actorUserId, actorRole, "SUCCESS", null, "{}");
    }

    public void recordFailure(
            String customerId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
        saveEvent(customerId, eventType, actorUserId, actorRole, "FAILURE", reasonCode, metadata);
    }

    private void saveEvent(
            String customerId,
            String eventType,
            String actorUserId,
            String actorRole,
            String outcome,
            String reasonCode,
            String metadata) {
        CustomerLifecycleEventEntity event = new CustomerLifecycleEventEntity();
        event.setCustomerId(customerId);
        event.setEventType(eventType.toUpperCase(Locale.ROOT));
        event.setActorUserId(actorUserId == null ? "anonymous" : actorUserId);
        event.setActorRole(actorRole == null ? "UNKNOWN" : actorRole.toUpperCase(Locale.ROOT));
        event.setOccurredAtUtc(Instant.now());
        event.setOutcome(outcome);
        event.setReasonCode(reasonCode);
        event.setMetadata(metadata == null ? "{}" : metadata);
        eventRepository.save(event);
    }
}

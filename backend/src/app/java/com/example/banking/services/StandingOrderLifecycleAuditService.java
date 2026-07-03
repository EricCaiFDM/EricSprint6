package com.example.banking.services;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.StandingOrderLifecycleEventJpaRepository;
import com.example.banking.models.StandingOrderLifecycleEventEntity;

@Service
public class StandingOrderLifecycleAuditService {
    private final StandingOrderLifecycleEventJpaRepository eventRepository;

    public StandingOrderLifecycleAuditService(StandingOrderLifecycleEventJpaRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void recordEvent(
            String standingOrderId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
        StandingOrderLifecycleEventEntity event = new StandingOrderLifecycleEventEntity();
        event.setStandingOrderId(standingOrderId);
        event.setEventType(eventType == null ? "UNKNOWN" : eventType.toUpperCase(Locale.ROOT));
        event.setActorUserId(actorUserId == null ? "anonymous" : actorUserId);
        event.setActorRole(actorRole == null ? "UNKNOWN" : actorRole.toUpperCase(Locale.ROOT));
        event.setOccurredAtUtc(Instant.now());
        event.setReasonCode(reasonCode);
        event.setMetadata(metadata == null ? "{}" : metadata);
        eventRepository.save(event);
    }
}

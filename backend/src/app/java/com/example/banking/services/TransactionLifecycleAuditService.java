package com.example.banking.services;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.TransactionLifecycleEventJpaRepository;
import com.example.banking.models.TransactionLifecycleEventEntity;

@Service
public class TransactionLifecycleAuditService {
    private final TransactionLifecycleEventJpaRepository eventRepository;

    public TransactionLifecycleAuditService(TransactionLifecycleEventJpaRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void recordSuccess(String transactionId, String eventType, String actorUserId, String actorRole, String metadata) {
        saveEvent(transactionId, eventType, actorUserId, actorRole, "SUCCESS", null, metadata);
    }

    public void recordFailure(
            String transactionId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
        saveEvent(transactionId, eventType, actorUserId, actorRole, "FAILURE", reasonCode, metadata);
    }

    private void saveEvent(
            String transactionId,
            String eventType,
            String actorUserId,
            String actorRole,
            String outcome,
            String reasonCode,
            String metadata) {
        TransactionLifecycleEventEntity event = new TransactionLifecycleEventEntity();
        event.setTransactionId(transactionId);
        event.setEventType(eventType == null ? "UNKNOWN" : eventType.toUpperCase(Locale.ROOT));
        event.setActorUserId(actorUserId == null ? "anonymous" : actorUserId);
        event.setActorRole(actorRole == null ? "UNKNOWN" : actorRole.toUpperCase(Locale.ROOT));
        event.setOccurredAtUtc(Instant.now());
        event.setOutcome(outcome);
        event.setReasonCode(reasonCode);
        event.setMetadata(metadata == null ? "{}" : metadata);
        eventRepository.save(event);
    }
}

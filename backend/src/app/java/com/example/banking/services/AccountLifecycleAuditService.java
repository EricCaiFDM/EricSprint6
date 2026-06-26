package com.example.banking.services;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AccountLifecycleEventJpaRepository;
import com.example.banking.models.AccountLifecycleEventEntity;

@Service
public class AccountLifecycleAuditService {
    private final AccountLifecycleEventJpaRepository eventRepository;

    public AccountLifecycleAuditService(AccountLifecycleEventJpaRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void recordSuccess(String accountId, String eventType, String actorUserId, String actorRole) {
        saveEvent(accountId, eventType, actorUserId, actorRole, "SUCCESS", null, "{}");
    }

    public void recordFailure(
            String accountId,
            String eventType,
            String actorUserId,
            String actorRole,
            String reasonCode,
            String metadata) {
        saveEvent(accountId, eventType, actorUserId, actorRole, "FAILURE", reasonCode, metadata);
    }

    private void saveEvent(
            String accountId,
            String eventType,
            String actorUserId,
            String actorRole,
            String outcome,
            String reasonCode,
            String metadata) {
        AccountLifecycleEventEntity event = new AccountLifecycleEventEntity();
        event.setAccountId(accountId);
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

package com.example.banking.services;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.api.notifications.schemas.TriggerNotificationSchema;
import com.example.banking.lib.errors.NotificationErrors;
import com.example.banking.lib.security.NotificationAccessPolicy;
import com.example.banking.lib.security.NotificationPreferencePolicy;
import com.example.banking.lib.security.NotificationTemplateSanitizer;
import com.example.banking.models.NotificationChannel;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationEventStatus;
import com.example.banking.models.NotificationPreferenceSnapshot;
import com.example.banking.models.NotificationPreferenceSnapshotEntity;
import com.example.banking.models.NotificationRecipientScopeType;
import com.example.banking.workers.notifications.NotificationDispatchWorker;

@Service
public class TriggerNotificationService {
    private final NotificationEventRepository eventRepository;
    private final NotificationPreferenceSnapshotRepository preferenceSnapshotRepository;
    private final NotificationTemplateSanitizer templateSanitizer;
    private final NotificationPreferencePolicy preferencePolicy;
    private final NotificationAccessPolicy accessPolicy;
    private final NotificationDispatchWorker dispatchWorker;

    public TriggerNotificationService(
            NotificationEventRepository eventRepository,
            NotificationPreferenceSnapshotRepository preferenceSnapshotRepository,
            NotificationTemplateSanitizer templateSanitizer,
            NotificationPreferencePolicy preferencePolicy,
            NotificationAccessPolicy accessPolicy,
            NotificationDispatchWorker dispatchWorker) {
        this.eventRepository = eventRepository;
        this.preferenceSnapshotRepository = preferenceSnapshotRepository;
        this.templateSanitizer = templateSanitizer;
        this.preferencePolicy = preferencePolicy;
        this.accessPolicy = accessPolicy;
        this.dispatchWorker = dispatchWorker;
    }

    @Transactional
    public NotificationEventEntity trigger(TriggerNotificationSchema request, String actorUserId, String role) {
        accessPolicy.enforceTriggerAccess(role);

        String normalizedScopeId = normalizeUuid(request.recipientScopeId(), "recipientScopeId");
        NotificationRecipientScopeType scopeType = normalizeScopeType(request.recipientScopeType());
        String normalizedActor = normalizeActor(actorUserId);

        accessPolicy.requireRecipientScope(scopeType, normalizedScopeId, role, normalizedActor, "trigger");

        String sanitizedTemplateContext = templateSanitizer.sanitize(request.templateContext());

        NotificationEventEntity event = new NotificationEventEntity();
        event.setEventType(normalizeRequired(request.eventType(), "eventType"));
        event.setRecipientScopeType(scopeType);
        event.setRecipientScopeId(normalizedScopeId);
        event.setTemplateCode(normalizeRequired(request.templateCode(), "templateCode"));
        event.setTemplateContext(sanitizedTemplateContext);
        event.setStatus(NotificationEventStatus.PENDING);

        NotificationEventEntity savedEvent = eventRepository.save(event);

        NotificationPreferenceSnapshot snapshot = preferencePolicy.evaluate(savedEvent, request.templateContext());
        NotificationPreferenceSnapshotEntity snapshotEntity = new NotificationPreferenceSnapshotEntity();
        snapshotEntity.setNotificationEventId(savedEvent.getNotificationEventId());
        snapshotEntity.setRecipientId(savedEvent.getRecipientScopeId());
        snapshotEntity.setConsentStatus(snapshot.consentStatus());
        snapshotEntity.setAllowedChannels(joinChannels(snapshot.allowedChannels()));
        snapshotEntity.setRestrictedChannels(joinChannels(snapshot.restrictedChannels()));
        preferenceSnapshotRepository.save(snapshotEntity);

        dispatchWorker.dispatch(savedEvent.getNotificationEventId());

        return eventRepository.findById(savedEvent.getNotificationEventId()).orElse(savedEvent);
    }

    private String joinChannels(List<NotificationChannel> channels) {
        if (channels == null || channels.isEmpty()) {
            return "";
        }
        return channels.stream().map(NotificationChannel::name).collect(Collectors.joining(","));
    }

    private NotificationRecipientScopeType normalizeScopeType(String value) {
        String normalized = normalizeRequired(value, "recipientScopeType").toUpperCase(Locale.ROOT);
        try {
            return NotificationRecipientScopeType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw NotificationErrors.validation(
                    "recipientScopeType must be CUSTOMER, ACCOUNT, or ADMIN",
                    "recipientScopeType");
        }
    }

    private String normalizeRequired(String value, String field) {
        if (value == null || value.isBlank()) {
            throw NotificationErrors.validation(field + " is required", field);
        }
        return value.trim();
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

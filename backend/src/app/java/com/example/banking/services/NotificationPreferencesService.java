package com.example.banking.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.banking.api.notifications.schemas.NotificationPreferencesRequestSchema;
import com.example.banking.api.notifications.schemas.NotificationPreferencesResponseSchema;
import com.example.banking.lib.security.NotificationAccessPolicy;

@Service
public class NotificationPreferencesService {
    private final NotificationAccessPolicy accessPolicy;
    private final Map<String, NotificationPreferencesResponseSchema> preferenceStore = new ConcurrentHashMap<>();

    public NotificationPreferencesService(NotificationAccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public NotificationPreferencesResponseSchema getPreferences(String actorUserId, String role) {
        accessPolicy.enforceTriggerAccess(role);
        return preferenceStore.computeIfAbsent(normalizeActor(actorUserId), ignored -> defaultPreferences());
    }

    public NotificationPreferencesResponseSchema updatePreferences(
            String actorUserId,
            String role,
            NotificationPreferencesRequestSchema request) {
        accessPolicy.enforceTriggerAccess(role);
        String actor = normalizeActor(actorUserId);

        NotificationPreferencesResponseSchema updated = new NotificationPreferencesResponseSchema(
            Boolean.TRUE.equals(request.depositAlertsEnabled()),
            Boolean.TRUE.equals(request.withdrawalAlertsEnabled()),
            Boolean.TRUE.equals(request.transferAlertsEnabled()),
            Boolean.TRUE.equals(request.statementAlertsEnabled()),
            Boolean.TRUE.equals(request.offersEnabled()));

        preferenceStore.put(actor, updated);
        return updated;
    }

    private NotificationPreferencesResponseSchema defaultPreferences() {
        return new NotificationPreferencesResponseSchema(true, true, true, true, false);
    }

    private String normalizeActor(String actorUserId) {
        if (actorUserId == null || actorUserId.isBlank()) {
            return "anonymous";
        }
        return actorUserId.trim();
    }
}

package com.example.banking.services;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.api.notifications.schemas.NotificationFeedItemSchema;
import com.example.banking.api.notifications.schemas.NotificationPreferencesResponseSchema;
import com.example.banking.lib.security.NotificationAccessPolicy;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationEventStatus;

@Service
public class ListRecentNotificationsService {
    private final NotificationEventRepository notificationEventRepository;
    private final NotificationAccessPolicy notificationAccessPolicy;
    private final NotificationPreferencesService notificationPreferencesService;

    public ListRecentNotificationsService(
            NotificationEventRepository notificationEventRepository,
            NotificationAccessPolicy notificationAccessPolicy,
            NotificationPreferencesService notificationPreferencesService) {
        this.notificationEventRepository = notificationEventRepository;
        this.notificationAccessPolicy = notificationAccessPolicy;
        this.notificationPreferencesService = notificationPreferencesService;
    }

    public List<NotificationFeedItemSchema> listRecent(int size, String actorUserId, String role) {
        int normalizedSize = Math.max(1, Math.min(size, 50));
        String normalizedActor = actorUserId == null || actorUserId.isBlank() ? "anonymous" : actorUserId.trim();
        NotificationPreferencesResponseSchema preferences = notificationPreferencesService.getPreferences(normalizedActor, role);

        int fetchSize = Math.max(25, normalizedSize * 4);
        while (true) {
            List<NotificationEventEntity> recentEvents = notificationEventRepository.listRecent(fetchSize);
            List<NotificationFeedItemSchema> visibleItems = recentEvents.stream()
                    .filter(event -> hasReadScope(event, normalizedActor, role))
                    .filter(event -> isEventEnabledByPreferences(event, preferences))
                    .limit(normalizedSize)
                    .map(this::toFeedItem)
                    .toList();

            if (visibleItems.size() >= normalizedSize || recentEvents.size() < fetchSize || fetchSize >= 5000) {
                return visibleItems;
            }

            fetchSize = Math.min(fetchSize * 2, 5000);
        }
    }

    private boolean isEventEnabledByPreferences(
            NotificationEventEntity event,
            NotificationPreferencesResponseSchema preferences) {
        String normalizedEventType = event.getEventType() == null
                ? ""
                : event.getEventType().trim().toUpperCase(Locale.ROOT);

        if (normalizedEventType.contains("DEPOSIT")) {
            return preferences.depositAlertsEnabled();
        }

        if (normalizedEventType.contains("WITHDRAWAL")) {
            return preferences.withdrawalAlertsEnabled();
        }

        if (normalizedEventType.contains("TRANSFER")) {
            return preferences.transferAlertsEnabled();
        }

        if (normalizedEventType.contains("STATEMENT")) {
            return preferences.statementAlertsEnabled();
        }

        if (normalizedEventType.contains("PROMOTION")
                || normalizedEventType.contains("PROMO")
                || normalizedEventType.contains("OFFER")
                || normalizedEventType.contains("MARKETING")) {
            return preferences.offersEnabled();
        }

        // Keep operational/security events visible unless explicitly mapped above.
        return true;
    }

    private boolean hasReadScope(NotificationEventEntity event, String actorUserId, String role) {
        try {
            notificationAccessPolicy.requireRecipientScope(
                    event.getRecipientScopeType(),
                    event.getRecipientScopeId(),
                    role,
                    actorUserId,
                    "read");
            return true;
        } catch (ApiErrorException exception) {
            return false;
        }
    }

    private NotificationFeedItemSchema toFeedItem(NotificationEventEntity event) {
        String title = toTitle(event.getEventType());

        NotificationEventStatus status = event.getStatus();
        String message;
        String level;
        if (status == NotificationEventStatus.COMPLETED) {
            message = "Delivered successfully";
            level = "Info";
        } else if (status == NotificationEventStatus.BLOCKED) {
            message = "Blocked by preferences";
            level = "Warning";
        } else if (status == NotificationEventStatus.FAILED) {
            message = "Delivery failed";
            level = "Warning";
        } else {
            message = "Processing";
            level = "Info";
        }

        String occurredAt = event.getCompletedAtUtc() == null
                ? event.getTriggeredAtUtc().toString()
                : event.getCompletedAtUtc().toString();

        return new NotificationFeedItemSchema(
                event.getNotificationEventId(),
                title,
                message,
                occurredAt,
                level);
    }

    private String toTitle(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            return "Notification";
        }

        String[] parts = eventType.trim().toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isBlank()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.length() == 0 ? "Notification" : builder.toString();
    }
}

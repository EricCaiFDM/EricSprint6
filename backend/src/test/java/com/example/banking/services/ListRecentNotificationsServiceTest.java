package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.banking.api.notifications.schemas.NotificationFeedItemSchema;
import com.example.banking.lib.errors.NotificationErrors;
import com.example.banking.lib.security.NotificationAccessPolicy;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationEventStatus;
import com.example.banking.models.NotificationRecipientScopeType;

class ListRecentNotificationsServiceTest {

    @Test
    void listRecentBuildsFeedItemsAndFiltersUnauthorizedEvents() {
        List<NotificationEventEntity> events = List.of(
                event("evt-1", "TRANSFER_COMPLETED", NotificationEventStatus.COMPLETED, "scope-1", Instant.parse("2026-06-30T08:00:00Z"), Instant.parse("2026-06-30T08:05:00Z")),
                event("evt-2", "PASSWORD_RESET", NotificationEventStatus.BLOCKED, "scope-2", Instant.parse("2026-06-30T09:00:00Z"), null),
                event("evt-3", "", NotificationEventStatus.FAILED, "scope-3", Instant.parse("2026-06-30T10:00:00Z"), null),
                event("evt-4", "PUSH_PROCESSING", NotificationEventStatus.PROCESSING, "scope-4", Instant.parse("2026-06-30T11:00:00Z"), null),
                event("evt-5", null, NotificationEventStatus.PROCESSING, "scope-5", Instant.parse("2026-06-30T12:00:00Z"), null));

        TrackingRecentRepository repository = new TrackingRecentRepository(events, events);
        TrackingNotificationAccessPolicy accessPolicy = new TrackingNotificationAccessPolicy(Set.of());

        ListRecentNotificationsService service = new ListRecentNotificationsService(repository, accessPolicy);
        List<NotificationFeedItemSchema> feed = service.listRecent(5, null, "CUSTOMER");

        assertEquals(5, feed.size());
        assertEquals("Transfer Completed", feed.get(0).title());
        assertEquals("Delivered successfully", feed.get(0).message());
        assertEquals("Info", feed.get(0).level());
        assertEquals("2026-06-30T08:05:00Z", feed.get(0).occurredAt());

        assertEquals("Password Reset", feed.get(1).title());
        assertEquals("Blocked by preferences", feed.get(1).message());

        assertEquals("Notification", feed.get(2).title());
        assertEquals("Delivery failed", feed.get(2).message());
        assertEquals("Warning", feed.get(2).level());

        assertEquals("Push Processing", feed.get(3).title());
        assertEquals("Processing", feed.get(3).message());

        assertEquals("Notification", feed.get(4).title());
        assertEquals("Processing", feed.get(4).message());

        assertEquals(List.of(25), repository.requestedSizes());
        assertTrue(accessPolicy.seenActors().stream().allMatch("anonymous"::equals));
    }

    @Test
    void listRecentExpandsFetchWindowUntilEnoughVisibleItems() {
        List<NotificationEventEntity> firstWindow = new ArrayList<>();
        for (int index = 0; index < 25; index++) {
            firstWindow.add(event(
                    "evt-first-" + index,
                    "EVENT_TYPE",
                    NotificationEventStatus.PROCESSING,
                    "first-scope-" + index,
                    Instant.parse("2026-06-30T12:00:00Z"),
                    null));
        }

        List<NotificationEventEntity> secondWindow = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            secondWindow.add(event(
                    "evt-second-" + index,
                    "EVENT_TYPE",
                    NotificationEventStatus.PROCESSING,
                    "second-scope-" + index,
                    Instant.parse("2026-06-30T13:00:00Z"),
                    null));
        }

        Set<String> forbiddenScopes = new HashSet<>();
        firstWindow.forEach(item -> forbiddenScopes.add(item.getRecipientScopeId()));
        secondWindow.stream().skip(3).forEach(item -> forbiddenScopes.add(item.getRecipientScopeId()));

        TrackingRecentRepository repository = new TrackingRecentRepository(firstWindow, secondWindow);
        TrackingNotificationAccessPolicy accessPolicy = new TrackingNotificationAccessPolicy(forbiddenScopes);

        ListRecentNotificationsService service = new ListRecentNotificationsService(repository, accessPolicy);
        List<NotificationFeedItemSchema> feed = service.listRecent(3, " actor-100 ", "CUSTOMER");

        assertEquals(3, feed.size());
        assertEquals(List.of(25, 50), repository.requestedSizes());
        assertTrue(accessPolicy.seenActors().stream().allMatch("actor-100"::equals));
    }

    @Test
    void listRecentUsesAnonymousForBlankActorAndStopsAtFetchSizeCap() {
        List<NotificationEventEntity> fullWindow = new ArrayList<>();
        for (int index = 0; index < 5000; index++) {
            fullWindow.add(event(
                    "evt-cap-" + index,
                    "__",
                    NotificationEventStatus.PROCESSING,
                    "scope-cap-" + index,
                    Instant.parse("2026-06-30T14:00:00Z"),
                    null));
        }

        TrackingRecentRepository repository = new TrackingRecentRepository(fullWindow, fullWindow);
        TrackingNotificationAccessPolicy accessPolicy = new TrackingNotificationAccessPolicy(Set.copyOf(
                fullWindow.stream().map(NotificationEventEntity::getRecipientScopeId).toList()));

        ListRecentNotificationsService service = new ListRecentNotificationsService(repository, accessPolicy);
        List<NotificationFeedItemSchema> feed = service.listRecent(60, "   ", "CUSTOMER");

        assertEquals(List.of(), feed);
        assertEquals(List.of(200, 400, 800, 1600, 3200, 5000), repository.requestedSizes());
        assertTrue(accessPolicy.seenActors().stream().allMatch("anonymous"::equals));
    }

    @Test
    void listRecentTitleParsingHandlesBlankTokensAndSingleCharacterParts() {
        List<NotificationEventEntity> events = List.of(
                event("evt-title-1", "___", NotificationEventStatus.PROCESSING, "scope-title-1", Instant.parse("2026-06-30T15:00:00Z"), null),
                event("evt-title-2", "a_b", NotificationEventStatus.PROCESSING, "scope-title-2", Instant.parse("2026-06-30T15:05:00Z"), null),
                event("evt-title-3", " _x_ ", NotificationEventStatus.PROCESSING, "scope-title-3", Instant.parse("2026-06-30T15:06:00Z"), null));

        TrackingRecentRepository repository = new TrackingRecentRepository(events, events);
        TrackingNotificationAccessPolicy accessPolicy = new TrackingNotificationAccessPolicy(Set.of());

        ListRecentNotificationsService service = new ListRecentNotificationsService(repository, accessPolicy);
        List<NotificationFeedItemSchema> feed = service.listRecent(3, "actor", "CUSTOMER");

        assertEquals(3, feed.size());
        assertEquals("Notification", feed.get(0).title());
        assertEquals("A B", feed.get(1).title());
        assertEquals("X", feed.get(2).title());
    }

    private NotificationEventEntity event(
            String id,
            String eventType,
            NotificationEventStatus status,
            String scopeId,
            Instant triggeredAt,
            Instant completedAt) {
        NotificationEventEntity event = new NotificationEventEntity();
        event.setNotificationEventId(id);
        event.setEventType(eventType);
        event.setRecipientScopeType(NotificationRecipientScopeType.CUSTOMER);
        event.setRecipientScopeId(scopeId);
        event.setStatus(status);
        event.setTriggeredAtUtc(triggeredAt);
        event.setCompletedAtUtc(completedAt);
        return event;
    }

    private static class TrackingRecentRepository implements NotificationEventRepository {
        private final List<NotificationEventEntity> firstWindow;
        private final List<NotificationEventEntity> secondWindow;
        private final List<Integer> requestedSizes = new ArrayList<>();

        private TrackingRecentRepository(List<NotificationEventEntity> firstWindow, List<NotificationEventEntity> secondWindow) {
            this.firstWindow = firstWindow;
            this.secondWindow = secondWindow;
        }

        @Override
        public NotificationEventEntity save(NotificationEventEntity entity) {
            return entity;
        }

        @Override
        public Optional<NotificationEventEntity> findById(String notificationEventId) {
            return Optional.empty();
        }

        @Override
        public List<NotificationEventEntity> listRecent(int size) {
            requestedSizes.add(size);
            if (size <= 25) {
                return firstWindow;
            }
            return secondWindow;
        }

        private List<Integer> requestedSizes() {
            return requestedSizes;
        }
    }

    private static class TrackingNotificationAccessPolicy extends NotificationAccessPolicy {
        private final Set<String> forbiddenScopeIds;
        private final List<String> seenActors = new ArrayList<>();

        private TrackingNotificationAccessPolicy(Set<String> forbiddenScopeIds) {
            super(null, null);
            this.forbiddenScopeIds = forbiddenScopeIds;
        }

        @Override
        public void requireRecipientScope(
                NotificationRecipientScopeType scopeType,
                String recipientScopeId,
                String role,
                String actorUserId,
                String operation) {
            seenActors.add(actorUserId);
            if (forbiddenScopeIds.contains(recipientScopeId)) {
                throw NotificationErrors.forbidden(operation);
            }
        }

        private List<String> seenActors() {
            return seenActors;
        }
    }
}
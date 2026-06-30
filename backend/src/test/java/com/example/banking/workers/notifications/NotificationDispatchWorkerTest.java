package com.example.banking.workers.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import com.example.banking.api.common.ApiErrorException;
import com.example.banking.lib.config.NotificationModuleConfig;
import com.example.banking.models.NotificationChannel;
import com.example.banking.models.NotificationConsentStatus;
import com.example.banking.models.NotificationDeliveryFinalStatus;
import com.example.banking.models.NotificationDeliveryOutcomeEntity;
import com.example.banking.models.NotificationDispatchAttemptEntity;
import com.example.banking.models.NotificationDispatchAttemptStatus;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationEventStatus;
import com.example.banking.models.NotificationPreferenceSnapshot;
import com.example.banking.models.NotificationPreferenceSnapshotEntity;
import com.example.banking.services.ChannelDispatchAdapter;
import com.example.banking.services.ChannelDispatchResult;
import com.example.banking.services.NotificationDeliveryOutcomeRepository;
import com.example.banking.services.NotificationDispatchAttemptRepository;
import com.example.banking.services.NotificationEventRepository;
import com.example.banking.services.NotificationPreferenceEnforcementService;
import com.example.banking.services.NotificationPreferenceSnapshotRepository;
import com.example.banking.services.NotificationRetryFallbackPolicyService;

class NotificationDispatchWorkerTest {

    private EventRepositoryDouble eventRepository;
    private SnapshotRepositoryDouble snapshotRepository;
    private AttemptRepositoryDouble attemptRepository;
    private OutcomeRepositoryDouble outcomeRepository;
    private RetryFallbackPolicyDouble retryFallbackPolicyService;
    private PreferenceEnforcementDouble preferenceEnforcementService;
    private ChannelDispatchAdapterDouble channelDispatchAdapter;
    private NotificationDispatchWorker worker;

    @BeforeEach
    void setUp() {
        eventRepository = new EventRepositoryDouble();
        snapshotRepository = new SnapshotRepositoryDouble();
        attemptRepository = new AttemptRepositoryDouble();
        outcomeRepository = new OutcomeRepositoryDouble();
        retryFallbackPolicyService = new RetryFallbackPolicyDouble();
        preferenceEnforcementService = new PreferenceEnforcementDouble();
        channelDispatchAdapter = new ChannelDispatchAdapterDouble();

        worker = new NotificationDispatchWorker(
                eventRepository,
                snapshotRepository,
                attemptRepository,
                outcomeRepository,
                retryFallbackPolicyService,
                preferenceEnforcementService,
                channelDispatchAdapter);
    }

    @Test
    void dispatchThrowsNotFoundWhenEventIsMissing() {
        ApiErrorException exception = captureDispatchError("missing-event");

        assertNotNull(exception);
        assertEquals("NOTIFICATION_EVENT_NOT_FOUND", exception.getCode());
        assertNull(exception.getField());
        assertEquals(0, eventRepository.saved.size());
    }

    @Test
    void dispatchReturnsImmediatelyForCompletedBlockedAndFailedStatuses() {
        eventRepository.put(event("event-completed", NotificationEventStatus.COMPLETED));
        eventRepository.put(event("event-blocked", NotificationEventStatus.BLOCKED));
        eventRepository.put(event("event-failed", NotificationEventStatus.FAILED));

        worker.dispatch("event-completed");
        worker.dispatch("event-blocked");
        worker.dispatch("event-failed");

        assertEquals(0, eventRepository.saved.size());
        assertEquals(0, attemptRepository.saved.size());
        assertEquals(0, outcomeRepository.saved.size());
    }

    @Test
    void dispatchThrowsConflictWhenSnapshotIsMissingAfterProcessingStarts() {
        eventRepository.put(event("event-no-snapshot", NotificationEventStatus.PENDING));

        ApiErrorException exception = captureDispatchError("event-no-snapshot");

        assertNotNull(exception);
        assertEquals("NOTIFICATION_CONFLICT", exception.getCode());
        assertEquals("notificationEventId", exception.getField());
        assertEquals(1, eventRepository.saved.size());
        assertEquals(NotificationEventStatus.PROCESSING, eventRepository.saved.get(0).getStatus());
    }

    @Test
    void dispatchPersistsBlockedOutcomeWhenDispatchOrderIsEmpty() {
        NotificationEventEntity event = event("event-empty-order", NotificationEventStatus.PENDING);
        eventRepository.put(event);
        snapshotRepository.put(snapshot("event-empty-order", NotificationConsentStatus.CONSENTED, "EMAIL", ""));
        retryFallbackPolicyService.dispatchOrder = List.of();

        worker.dispatch("event-empty-order");

        assertEquals(2, eventRepository.saved.size());
        assertEquals(NotificationEventStatus.BLOCKED, event.getStatus());
        assertNotNull(event.getCompletedAtUtc());

        assertEquals(1, outcomeRepository.saved.size());
        NotificationDeliveryOutcomeEntity outcome = outcomeRepository.saved.get(0);
        assertEquals(NotificationDeliveryFinalStatus.BLOCKED_RESTRICTED, outcome.getFinalStatus());
        assertEquals("CONSENT_RESTRICTED", outcome.getReasonCode());

        assertNotNull(retryFallbackPolicyService.lastSnapshot);
        assertEquals(List.of(NotificationChannel.EMAIL), retryFallbackPolicyService.lastSnapshot.allowedChannels());
        assertTrue(retryFallbackPolicyService.lastSnapshot.restrictedChannels().isEmpty());
    }

    @Test
    void dispatchTreatsNullChannelListsInSnapshotAsEmpty() {
        NotificationEventEntity event = event("event-null-channels", NotificationEventStatus.PENDING);
        eventRepository.put(event);
        snapshotRepository.put(snapshot("event-null-channels", NotificationConsentStatus.CONSENTED, null, null));
        retryFallbackPolicyService.dispatchOrder = List.of();

        worker.dispatch("event-null-channels");

        assertNotNull(retryFallbackPolicyService.lastSnapshot);
        assertTrue(retryFallbackPolicyService.lastSnapshot.allowedChannels().isEmpty());
        assertTrue(retryFallbackPolicyService.lastSnapshot.restrictedChannels().isEmpty());
        assertEquals(NotificationEventStatus.BLOCKED, event.getStatus());
    }

    @Test
    void dispatchAllRestrictedChannelsSavesBlockedAttemptsAndBlockedOutcome() {
        NotificationEventEntity event = event("event-restricted", NotificationEventStatus.PENDING);
        eventRepository.put(event);
        snapshotRepository.put(snapshot(
                "event-restricted",
                NotificationConsentStatus.CONSENTED,
                " EMAIL, ,SMS ",
                " PUSH , "));

        retryFallbackPolicyService.dispatchOrder = List.of(NotificationChannel.EMAIL, NotificationChannel.SMS);
        preferenceEnforcementService.setDecision(
                NotificationChannel.EMAIL,
                new NotificationPreferenceEnforcementService.EnforcementDecision(true, "CHANNEL_RESTRICTED"));
        preferenceEnforcementService.setDecision(
                NotificationChannel.SMS,
                new NotificationPreferenceEnforcementService.EnforcementDecision(true, "CONSENT_RESTRICTED"));

        worker.dispatch("event-restricted");

        assertEquals(2, attemptRepository.saved.size());
        NotificationDispatchAttemptEntity first = attemptRepository.saved.get(0);
        NotificationDispatchAttemptEntity second = attemptRepository.saved.get(1);

        assertEquals(1, first.getAttemptNumber());
        assertEquals(NotificationDispatchAttemptStatus.FAILED_RESTRICTED, first.getStatus());
        assertEquals("CHANNEL_RESTRICTED", first.getReasonCode());
        assertNotNull(first.getStartedAtUtc());
        assertNotNull(first.getCompletedAtUtc());

        assertEquals(2, second.getAttemptNumber());
        assertEquals(NotificationDispatchAttemptStatus.FAILED_RESTRICTED, second.getStatus());
        assertEquals("CONSENT_RESTRICTED", second.getReasonCode());

        assertEquals(1, outcomeRepository.saved.size());
        assertEquals(NotificationDeliveryFinalStatus.BLOCKED_RESTRICTED, outcomeRepository.saved.get(0).getFinalStatus());
        assertEquals("CONSENT_RESTRICTED", outcomeRepository.saved.get(0).getReasonCode());
        assertEquals(NotificationEventStatus.BLOCKED, event.getStatus());

        assertNotNull(retryFallbackPolicyService.lastSnapshot);
        assertEquals(List.of(NotificationChannel.EMAIL, NotificationChannel.SMS), retryFallbackPolicyService.lastSnapshot.allowedChannels());
        assertEquals(List.of(NotificationChannel.PUSH), retryFallbackPolicyService.lastSnapshot.restrictedChannels());
    }

    @Test
    void dispatchSuccessfulFirstAttemptCompletesEventAndBreaksLoop() {
        NotificationEventEntity event = event("event-success", NotificationEventStatus.PENDING);
        eventRepository.put(event);
        snapshotRepository.put(snapshot("event-success", NotificationConsentStatus.CONSENTED, "EMAIL,SMS", ""));

        retryFallbackPolicyService.dispatchOrder = List.of(NotificationChannel.EMAIL, NotificationChannel.SMS);
        channelDispatchAdapter.queue(new ChannelDispatchResult(
                NotificationDispatchAttemptStatus.SUCCEEDED,
                null,
                "provider-1",
                false,
                true));

        worker.dispatch("event-success");

        assertEquals(1, attemptRepository.saved.size());
        NotificationDispatchAttemptEntity attempt = attemptRepository.saved.get(0);
        assertEquals(NotificationDispatchAttemptStatus.SUCCEEDED, attempt.getStatus());
        assertEquals("provider-1", attempt.getProviderReferenceId());

        assertEquals(1, outcomeRepository.saved.size());
        NotificationDeliveryOutcomeEntity outcome = outcomeRepository.saved.get(0);
        assertEquals(NotificationDeliveryFinalStatus.DELIVERED, outcome.getFinalStatus());
        assertEquals(NotificationChannel.EMAIL, outcome.getDeliveredChannel());
        assertNull(outcome.getReasonCode());

        assertEquals(NotificationEventStatus.COMPLETED, event.getStatus());
        assertNotNull(event.getCompletedAtUtc());
        assertEquals(1, channelDispatchAdapter.calls.size());
    }

    @Test
    void dispatchRetryScheduledThenDeliveredOnFallbackChannel() {
        NotificationEventEntity event = event("event-retry", NotificationEventStatus.PENDING);
        eventRepository.put(event);
        snapshotRepository.put(snapshot("event-retry", NotificationConsentStatus.CONSENTED, "EMAIL,SMS", ""));

        retryFallbackPolicyService.dispatchOrder = List.of(NotificationChannel.EMAIL, NotificationChannel.SMS);
        retryFallbackPolicyService.shouldRetryDecision = true;
        retryFallbackPolicyService.nextRetryAtValue = Instant.parse("2026-06-30T12:34:56Z");

        channelDispatchAdapter.queue(new ChannelDispatchResult(
                NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE,
                "TEMP_UNAVAILABLE",
                "provider-1",
                true,
                false));
        channelDispatchAdapter.queue(new ChannelDispatchResult(
                NotificationDispatchAttemptStatus.SUCCEEDED,
                null,
                "provider-2",
                false,
                true));

        worker.dispatch("event-retry");

        assertEquals(2, attemptRepository.saved.size());
        NotificationDispatchAttemptEntity first = attemptRepository.saved.get(0);
        NotificationDispatchAttemptEntity second = attemptRepository.saved.get(1);

        assertEquals(NotificationDispatchAttemptStatus.RETRY_SCHEDULED, first.getStatus());
        assertEquals("RETRY_SCHEDULED_2026-06-30T12:34:56Z", first.getReasonCode());
        assertEquals(NotificationDispatchAttemptStatus.SUCCEEDED, second.getStatus());

        assertEquals(1, outcomeRepository.saved.size());
        assertEquals(NotificationDeliveryFinalStatus.DELIVERED, outcomeRepository.saved.get(0).getFinalStatus());
        assertEquals(NotificationChannel.SMS, outcomeRepository.saved.get(0).getDeliveredChannel());

        assertTrue(retryFallbackPolicyService.shouldRetryCalled);
        assertEquals(1, retryFallbackPolicyService.lastAttemptNumber);
        assertEquals(NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE, retryFallbackPolicyService.lastAttemptStatus);
    }

    @Test
    void dispatchNonRetryFailureEndsAsFailedOutcome() {
        NotificationEventEntity event = event("event-failure", NotificationEventStatus.PENDING);
        eventRepository.put(event);
        snapshotRepository.put(snapshot("event-failure", NotificationConsentStatus.CONSENTED, "PUSH", ""));

        retryFallbackPolicyService.dispatchOrder = List.of(NotificationChannel.PUSH);
        retryFallbackPolicyService.shouldRetryDecision = false;

        channelDispatchAdapter.queue(new ChannelDispatchResult(
                NotificationDispatchAttemptStatus.FAILED_TEMPLATE_RESOLUTION,
                "TEMPLATE_INVALID",
                "provider-x",
                false,
                false));

        worker.dispatch("event-failure");

        assertEquals(1, attemptRepository.saved.size());
        assertEquals(
                NotificationDispatchAttemptStatus.FAILED_TEMPLATE_RESOLUTION,
                attemptRepository.saved.get(0).getStatus());
        assertEquals("TEMPLATE_INVALID", attemptRepository.saved.get(0).getReasonCode());

        assertEquals(1, outcomeRepository.saved.size());
        NotificationDeliveryOutcomeEntity outcome = outcomeRepository.saved.get(0);
        assertEquals(NotificationDeliveryFinalStatus.FAILED, outcome.getFinalStatus());
        assertEquals("DELIVERY_FAILED", outcome.getReasonCode());

        assertEquals(NotificationEventStatus.FAILED, event.getStatus());
        assertNotNull(event.getCompletedAtUtc());
        assertFalse(retryFallbackPolicyService.shouldRetryDecision);
    }

    private ApiErrorException captureDispatchError(String notificationEventId) {
        ApiErrorException exception = null;
        try {
            worker.dispatch(notificationEventId);
        } catch (ApiErrorException captured) {
            exception = captured;
        }
        return exception;
    }

    private NotificationEventEntity event(String eventId, NotificationEventStatus status) {
        NotificationEventEntity event = new NotificationEventEntity();
        event.setNotificationEventId(eventId);
        event.setEventType("ACCOUNT_ALERT");
        event.setTemplateCode("TEMPLATE_001");
        event.setTemplateContext("{}");
        event.setStatus(status);
        event.setTriggeredAtUtc(Instant.parse("2026-06-30T10:00:00Z"));
        return event;
    }

    private NotificationPreferenceSnapshotEntity snapshot(
            String eventId,
            NotificationConsentStatus consentStatus,
            String allowedChannels,
            String restrictedChannels) {
        NotificationPreferenceSnapshotEntity snapshot = new NotificationPreferenceSnapshotEntity();
        snapshot.setSnapshotId(UUID.randomUUID().toString());
        snapshot.setNotificationEventId(eventId);
        snapshot.setRecipientId("recipient-1");
        snapshot.setConsentStatus(consentStatus);
        snapshot.setAllowedChannels(allowedChannels);
        snapshot.setRestrictedChannels(restrictedChannels);
        snapshot.setCapturedAtUtc(Instant.parse("2026-06-30T10:00:00Z"));
        return snapshot;
    }

    private static final class EventRepositoryDouble implements NotificationEventRepository {
        private final Map<String, NotificationEventEntity> byId = new HashMap<>();
        private final List<NotificationEventEntity> saved = new ArrayList<>();

        private void put(NotificationEventEntity entity) {
            byId.put(entity.getNotificationEventId(), entity);
        }

        @Override
        public NotificationEventEntity save(NotificationEventEntity entity) {
            saved.add(entity);
            byId.put(entity.getNotificationEventId(), entity);
            return entity;
        }

        @Override
        public Optional<NotificationEventEntity> findById(String notificationEventId) {
            return Optional.ofNullable(byId.get(notificationEventId));
        }

        @Override
        public List<NotificationEventEntity> listRecent(int size) {
            return List.of();
        }
    }

    private static final class SnapshotRepositoryDouble implements NotificationPreferenceSnapshotRepository {
        private final Map<String, NotificationPreferenceSnapshotEntity> byEventId = new HashMap<>();

        private void put(NotificationPreferenceSnapshotEntity entity) {
            byEventId.put(entity.getNotificationEventId(), entity);
        }

        @Override
        public NotificationPreferenceSnapshotEntity save(NotificationPreferenceSnapshotEntity entity) {
            byEventId.put(entity.getNotificationEventId(), entity);
            return entity;
        }

        @Override
        public Optional<NotificationPreferenceSnapshotEntity> findByEventId(String notificationEventId) {
            return Optional.ofNullable(byEventId.get(notificationEventId));
        }
    }

    private static final class AttemptRepositoryDouble implements NotificationDispatchAttemptRepository {
        private final List<NotificationDispatchAttemptEntity> saved = new ArrayList<>();

        @Override
        public NotificationDispatchAttemptEntity save(NotificationDispatchAttemptEntity entity) {
            saved.add(entity);
            return entity;
        }

        @Override
        public Page<NotificationDispatchAttemptEntity> listByEventId(String notificationEventId, int page, int pageSize) {
            return Page.empty();
        }

        @Override
        public long countByEventId(String notificationEventId) {
            return saved.stream()
                    .filter(item -> notificationEventId.equals(item.getNotificationEventId()))
                    .count();
        }
    }

    private static final class OutcomeRepositoryDouble implements NotificationDeliveryOutcomeRepository {
        private final List<NotificationDeliveryOutcomeEntity> saved = new ArrayList<>();

        @Override
        public NotificationDeliveryOutcomeEntity save(NotificationDeliveryOutcomeEntity entity) {
            saved.add(entity);
            return entity;
        }

        @Override
        public Optional<NotificationDeliveryOutcomeEntity> findByEventId(String notificationEventId) {
            return saved.stream()
                    .filter(item -> notificationEventId.equals(item.getNotificationEventId()))
                    .findFirst();
        }
    }

    private static final class RetryFallbackPolicyDouble extends NotificationRetryFallbackPolicyService {
        private List<NotificationChannel> dispatchOrder = List.of();
        private NotificationPreferenceSnapshot lastSnapshot;
        private boolean shouldRetryDecision;
        private boolean shouldRetryCalled;
        private int lastAttemptNumber;
        private NotificationDispatchAttemptStatus lastAttemptStatus;
        private Instant nextRetryAtValue = Instant.parse("2026-06-30T11:00:00Z");

        private RetryFallbackPolicyDouble() {
            super(new NotificationModuleConfig());
        }

        @Override
        public List<NotificationChannel> resolveDispatchOrder(NotificationPreferenceSnapshot snapshot) {
            lastSnapshot = snapshot;
            return dispatchOrder;
        }

        @Override
        public boolean shouldRetry(int attemptNumber, NotificationDispatchAttemptStatus status) {
            shouldRetryCalled = true;
            lastAttemptNumber = attemptNumber;
            lastAttemptStatus = status;
            return shouldRetryDecision;
        }

        @Override
        public Instant nextRetryAt(Instant from) {
            return nextRetryAtValue;
        }
    }

    private static final class PreferenceEnforcementDouble extends NotificationPreferenceEnforcementService {
        private final Map<NotificationChannel, EnforcementDecision> byChannel = new HashMap<>();

        private void setDecision(NotificationChannel channel, EnforcementDecision decision) {
            byChannel.put(channel, decision);
        }

        @Override
        public EnforcementDecision evaluate(NotificationPreferenceSnapshot snapshot, NotificationChannel channel) {
            return byChannel.getOrDefault(channel, new EnforcementDecision(false, null));
        }
    }

    private static final class ChannelDispatchAdapterDouble implements ChannelDispatchAdapter {
        private final List<ChannelDispatchResult> scriptedResults = new ArrayList<>();
        private final List<String> calls = new ArrayList<>();

        private void queue(ChannelDispatchResult result) {
            scriptedResults.add(result);
        }

        @Override
        public ChannelDispatchResult dispatch(
                NotificationChannel channel,
                String templateCode,
                String sanitizedTemplateContext,
                String notificationEventId,
                int attemptNumber) {
            calls.add(channel + "|" + attemptNumber);
            if (!scriptedResults.isEmpty()) {
                return scriptedResults.remove(0);
            }
            return new ChannelDispatchResult(
                    NotificationDispatchAttemptStatus.FAILED_TEMPLATE_RESOLUTION,
                    "UNSCRIPTED",
                    null,
                    false,
                    false);
        }
    }
}
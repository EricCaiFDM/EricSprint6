package com.example.banking.workers.notifications;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banking.lib.errors.NotificationErrors;
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

@Service
public class NotificationDispatchWorker {
    private final NotificationEventRepository eventRepository;
    private final NotificationPreferenceSnapshotRepository snapshotRepository;
    private final NotificationDispatchAttemptRepository attemptRepository;
    private final NotificationDeliveryOutcomeRepository outcomeRepository;
    private final NotificationRetryFallbackPolicyService retryFallbackPolicyService;
    private final NotificationPreferenceEnforcementService preferenceEnforcementService;
    private final ChannelDispatchAdapter channelDispatchAdapter;

    public NotificationDispatchWorker(
            NotificationEventRepository eventRepository,
            NotificationPreferenceSnapshotRepository snapshotRepository,
            NotificationDispatchAttemptRepository attemptRepository,
            NotificationDeliveryOutcomeRepository outcomeRepository,
            NotificationRetryFallbackPolicyService retryFallbackPolicyService,
            NotificationPreferenceEnforcementService preferenceEnforcementService,
            ChannelDispatchAdapter channelDispatchAdapter) {
        this.eventRepository = eventRepository;
        this.snapshotRepository = snapshotRepository;
        this.attemptRepository = attemptRepository;
        this.outcomeRepository = outcomeRepository;
        this.retryFallbackPolicyService = retryFallbackPolicyService;
        this.preferenceEnforcementService = preferenceEnforcementService;
        this.channelDispatchAdapter = channelDispatchAdapter;
    }

    @Transactional
    public void dispatch(String notificationEventId) {
        NotificationEventEntity event = eventRepository.findById(notificationEventId)
                .orElseThrow(() -> NotificationErrors.notFound(notificationEventId));

        if (event.getStatus() == NotificationEventStatus.COMPLETED
                || event.getStatus() == NotificationEventStatus.BLOCKED
                || event.getStatus() == NotificationEventStatus.FAILED) {
            return;
        }

        event.setStatus(NotificationEventStatus.PROCESSING);
        eventRepository.save(event);

        NotificationPreferenceSnapshotEntity snapshotEntity = snapshotRepository.findByEventId(notificationEventId)
                .orElseThrow(() -> NotificationErrors.conflict(
                        "Preference snapshot is required before dispatch",
                        "notificationEventId"));

        NotificationPreferenceSnapshot snapshot = toSnapshot(snapshotEntity);
        List<NotificationChannel> dispatchOrder = retryFallbackPolicyService.resolveDispatchOrder(snapshot);

        if (dispatchOrder.isEmpty()) {
            persistBlockedOutcome(event, "CONSENT_RESTRICTED");
            return;
        }

        boolean delivered = false;
        boolean anyNonRestrictedFailure = false;
        int attemptNumber = 1;

        for (NotificationChannel channel : dispatchOrder) {
            NotificationPreferenceEnforcementService.EnforcementDecision enforcement = preferenceEnforcementService
                    .evaluate(snapshot, channel);
            if (enforcement.blocked()) {
                NotificationDispatchAttemptEntity blockedAttempt = new NotificationDispatchAttemptEntity();
                blockedAttempt.setNotificationEventId(notificationEventId);
                blockedAttempt.setChannel(channel);
                blockedAttempt.setAttemptNumber(attemptNumber++);
                blockedAttempt.setStartedAtUtc(Instant.now());
                blockedAttempt.setCompletedAtUtc(Instant.now());
                blockedAttempt.setStatus(NotificationDispatchAttemptStatus.FAILED_RESTRICTED);
                blockedAttempt.setReasonCode(enforcement.reasonCode());
                attemptRepository.save(blockedAttempt);
                continue;
            }

            NotificationDispatchAttemptEntity attempt = new NotificationDispatchAttemptEntity();
            attempt.setNotificationEventId(notificationEventId);
            attempt.setChannel(channel);
            attempt.setAttemptNumber(attemptNumber++);
            attempt.setStartedAtUtc(Instant.now());

            ChannelDispatchResult result = channelDispatchAdapter.dispatch(
                    channel,
                    event.getTemplateCode(),
                    event.getTemplateContext(),
                    notificationEventId,
                    attempt.getAttemptNumber());

            attempt.setCompletedAtUtc(Instant.now());
            attempt.setReasonCode(result.reasonCode());
            attempt.setProviderReferenceId(result.providerReferenceId());

            if (result.success()) {
                attempt.setStatus(NotificationDispatchAttemptStatus.SUCCEEDED);
                attemptRepository.save(attempt);

                NotificationDeliveryOutcomeEntity outcome = new NotificationDeliveryOutcomeEntity();
                outcome.setNotificationEventId(notificationEventId);
                outcome.setFinalStatus(NotificationDeliveryFinalStatus.DELIVERED);
                outcome.setDeliveredChannel(channel);
                outcomeRepository.save(outcome);

                event.setStatus(NotificationEventStatus.COMPLETED);
                event.setCompletedAtUtc(Instant.now());
                eventRepository.save(event);
                delivered = true;
                break;
            }

            anyNonRestrictedFailure = true;
            if (retryFallbackPolicyService.shouldRetry(attempt.getAttemptNumber(), result.status())) {
                attempt.setStatus(NotificationDispatchAttemptStatus.RETRY_SCHEDULED);
                attempt.setReasonCode("RETRY_SCHEDULED_" + retryFallbackPolicyService.nextRetryAt(Instant.now()));
            } else {
                attempt.setStatus(result.status());
            }
            attemptRepository.save(attempt);
        }

        if (!delivered) {
            NotificationDeliveryOutcomeEntity outcome = new NotificationDeliveryOutcomeEntity();
            outcome.setNotificationEventId(notificationEventId);

            if (anyNonRestrictedFailure) {
                outcome.setFinalStatus(NotificationDeliveryFinalStatus.FAILED);
                outcome.setReasonCode("DELIVERY_FAILED");
                event.setStatus(NotificationEventStatus.FAILED);
            } else {
                outcome.setFinalStatus(NotificationDeliveryFinalStatus.BLOCKED_RESTRICTED);
                outcome.setReasonCode("CONSENT_RESTRICTED");
                event.setStatus(NotificationEventStatus.BLOCKED);
            }

            outcomeRepository.save(outcome);
            event.setCompletedAtUtc(Instant.now());
            eventRepository.save(event);
        }
    }

    private void persistBlockedOutcome(NotificationEventEntity event, String reasonCode) {
        NotificationDeliveryOutcomeEntity outcome = new NotificationDeliveryOutcomeEntity();
        outcome.setNotificationEventId(event.getNotificationEventId());
        outcome.setFinalStatus(NotificationDeliveryFinalStatus.BLOCKED_RESTRICTED);
        outcome.setReasonCode(reasonCode);
        outcomeRepository.save(outcome);

        event.setStatus(NotificationEventStatus.BLOCKED);
        event.setCompletedAtUtc(Instant.now());
        eventRepository.save(event);
    }

    private NotificationPreferenceSnapshot toSnapshot(NotificationPreferenceSnapshotEntity entity) {
        List<NotificationChannel> allowed = splitChannels(entity.getAllowedChannels());
        List<NotificationChannel> restricted = splitChannels(entity.getRestrictedChannels());
        return new NotificationPreferenceSnapshot(entity.getConsentStatus(), allowed, restricted);
    }

    private List<NotificationChannel> splitChannels(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(NotificationChannel::valueOf)
                .toList();
    }
}

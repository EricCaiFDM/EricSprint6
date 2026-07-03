package com.example.banking.api.notifications;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.banking.api.notifications.schemas.NotificationAttemptItemSchema;
import com.example.banking.api.notifications.schemas.NotificationEventAcceptedResponseSchema;
import com.example.banking.api.notifications.schemas.NotificationEventResponseSchema;
import com.example.banking.models.NotificationDispatchAttemptEntity;
import com.example.banking.models.NotificationEvent;
import com.example.banking.models.NotificationEventEntity;

@Component
public class NotificationResponseMapper {
    public NotificationEventAcceptedResponseSchema toAcceptedResponse(NotificationEventEntity entity) {
        return new NotificationEventAcceptedResponseSchema(
                entity.getNotificationEventId(),
                entity.getStatus().name());
    }

    public NotificationEventResponseSchema toEventResponse(NotificationEvent event) {
        return new NotificationEventResponseSchema(
                event.notificationEventId(),
                event.eventType(),
                event.recipientScopeType().name(),
                event.recipientScopeId(),
                event.status().name(),
                event.finalOutcome() == null ? null : event.finalOutcome().name(),
                event.reasonCode(),
                event.triggeredAtUtc().toString(),
                event.completedAtUtc() == null ? null : event.completedAtUtc().toString());
    }

    public NotificationAttemptItemSchema toAttemptItem(NotificationDispatchAttemptEntity entity) {
        return new NotificationAttemptItemSchema(
                entity.getAttemptId(),
                entity.getChannel().name(),
                entity.getAttemptNumber(),
                entity.getStatus().name(),
                entity.getQueuedAtUtc().toString(),
                entity.getStartedAtUtc() == null ? null : entity.getStartedAtUtc().toString(),
                entity.getCompletedAtUtc() == null ? null : entity.getCompletedAtUtc().toString(),
                entity.getReasonCode(),
                entity.getProviderReferenceId());
    }

    public List<NotificationAttemptItemSchema> toAttemptItems(List<NotificationDispatchAttemptEntity> entities) {
        return entities.stream().map(this::toAttemptItem).toList();
    }
}

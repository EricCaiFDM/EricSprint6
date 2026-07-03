package com.example.banking.api.standingorders;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.banking.api.standingorders.schemas.StandingOrderExecutionItemSchema;
import com.example.banking.api.standingorders.schemas.StandingOrderResponseSchema;
import com.example.banking.models.StandingOrderEntity;
import com.example.banking.models.StandingOrderExecutionEventEntity;

@Component
public class StandingOrderResponseMapper {
    public StandingOrderResponseSchema toResponse(StandingOrderEntity entity) {
        return new StandingOrderResponseSchema(
                entity.getStandingOrderId(),
                entity.getSourceAccountId(),
                entity.getDestinationAccountId(),
                entity.getAmount().toPlainString(),
                entity.getCadence().name(),
                entity.getLifecycleState().name(),
                entity.getNextExecutionAtUtc() == null ? null : entity.getNextExecutionAtUtc().toString(),
                entity.getEffectiveFromUtc().toString(),
                entity.getEffectiveToUtc() == null ? null : entity.getEffectiveToUtc().toString());
    }

    public StandingOrderExecutionItemSchema toExecutionItem(StandingOrderExecutionEventEntity event) {
        return new StandingOrderExecutionItemSchema(
                event.getExecutionEventId(),
                event.getDueAtUtc().toString(),
                event.getStartedAtUtc().toString(),
                event.getCompletedAtUtc() == null ? null : event.getCompletedAtUtc().toString(),
                event.getStatus().name(),
                event.getAttemptNumber() == null ? 0 : event.getAttemptNumber(),
                event.getTransferReferenceId(),
                event.getReasonCode());
    }

    public List<StandingOrderExecutionItemSchema> toExecutionItems(List<StandingOrderExecutionEventEntity> events) {
        return events.stream().map(this::toExecutionItem).toList();
    }
}

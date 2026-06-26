package com.example.banking.api.standingorders.schemas;

public record StandingOrderExecutionItemSchema(
        String executionEventId,
        String dueAtUtc,
        String startedAtUtc,
        String completedAtUtc,
        String status,
        int attemptNumber,
        String transferReferenceId,
        String reasonCode) {
}

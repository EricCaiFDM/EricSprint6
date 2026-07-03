package com.example.banking.api.standingorders.schemas;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for standing order execution item schema.")
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

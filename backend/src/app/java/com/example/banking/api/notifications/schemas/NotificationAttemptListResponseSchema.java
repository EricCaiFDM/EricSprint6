package com.example.banking.api.notifications.schemas;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for notification attempt list response schema payload.")
public record NotificationAttemptListResponseSchema(
        List<NotificationAttemptItemSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

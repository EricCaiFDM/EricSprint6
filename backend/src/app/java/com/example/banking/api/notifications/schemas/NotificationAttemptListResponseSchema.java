package com.example.banking.api.notifications.schemas;

import java.util.List;

public record NotificationAttemptListResponseSchema(
        List<NotificationAttemptItemSchema> items,
        int page,
        int pageSize,
        long totalItems,
        int totalPages) {
}

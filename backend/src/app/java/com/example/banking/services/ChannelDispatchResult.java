package com.example.banking.services;

import com.example.banking.models.NotificationDispatchAttemptStatus;

public record ChannelDispatchResult(
        NotificationDispatchAttemptStatus status,
        String reasonCode,
        String providerReferenceId,
        boolean retryable,
        boolean success) {
}

package com.example.banking.models;

public enum NotificationDispatchAttemptStatus {
    SUCCEEDED,
    FAILED_CHANNEL_UNAVAILABLE,
    FAILED_TEMPLATE_RESOLUTION,
    FAILED_RESTRICTED,
    RETRY_SCHEDULED
}

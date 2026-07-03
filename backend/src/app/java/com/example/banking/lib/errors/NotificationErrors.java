package com.example.banking.lib.errors;

import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;

public final class NotificationErrors {
    private NotificationErrors() {
    }

    public static ApiErrorException validation(String message, String field) {
        return new ApiErrorException(HttpStatus.BAD_REQUEST, "NOTIFICATION_VALIDATION_ERROR", message, field);
    }

    public static ApiErrorException notFound(String notificationEventId) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "NOTIFICATION_EVENT_NOT_FOUND",
                "No notification event found for the provided identifier",
                notificationEventId == null ? "notificationEventId" : null);
    }

    public static ApiErrorException scopeNotFound(String field) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "NOTIFICATION_SCOPE_NOT_FOUND",
                "No recipient scope found for the provided identifier",
                field);
    }

    public static ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "NOTIFICATION_FORBIDDEN",
                "Insufficient privileges to " + operation + " notifications",
                null);
    }

    public static ApiErrorException conflict(String message, String field) {
        return new ApiErrorException(HttpStatus.CONFLICT, "NOTIFICATION_CONFLICT", message, field);
    }
}

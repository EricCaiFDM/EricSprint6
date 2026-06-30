package com.example.banking.lib.errors;

import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;

public final class InsightErrors {
    private InsightErrors() {
    }

    public static ApiErrorException validation(String message, String field) {
        return new ApiErrorException(HttpStatus.BAD_REQUEST, "INSIGHT_VALIDATION_ERROR", message, field);
    }

    public static ApiErrorException forbidden() {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "INSIGHT_FORBIDDEN",
                "Insufficient privileges to access spending insights for this scope",
                null);
    }

    public static ApiErrorException scopeNotFound(String field) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "INSIGHT_SCOPE_NOT_FOUND",
                "Requested insight scope was not found",
                field);
    }

    public static ApiErrorException dependencyFailure(String message) {
        return new ApiErrorException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "INSIGHT_DEPENDENCY_FAILURE",
                message,
                null);
    }
}

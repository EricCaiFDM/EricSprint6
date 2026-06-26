package com.example.banking.lib.errors;

import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;

public final class TransactionErrors {
    private TransactionErrors() {
    }

    public static ApiErrorException validation(String message, String field) {
        return new ApiErrorException(HttpStatus.BAD_REQUEST, "TRANSACTION_VALIDATION_ERROR", message, field);
    }

    public static ApiErrorException accountNotFound(String field) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "TRANSACTION_ACCOUNT_NOT_FOUND",
                "No account found for the provided identifier",
                field);
    }

    public static ApiErrorException scopeNotFound(String field, String message) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "TRANSACTION_SCOPE_NOT_FOUND",
                message,
                field);
    }

    public static ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "TRANSACTION_FORBIDDEN",
                "Insufficient privileges to " + operation + " transactions",
                null);
    }

    public static ApiErrorException conflict(String message, String field) {
        return new ApiErrorException(HttpStatus.CONFLICT, "TRANSACTION_CONFLICT", message, field);
    }

    public static ApiErrorException idempotencyConflict(String message) {
        return new ApiErrorException(HttpStatus.CONFLICT, "TRANSACTION_IDEMPOTENCY_CONFLICT", message, "Idempotency-Key");
    }

    public static ApiErrorException insufficientFunds() {
        return new ApiErrorException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "TRANSACTION_INSUFFICIENT_FUNDS",
                "Insufficient funds for this operation",
                null);
    }
}

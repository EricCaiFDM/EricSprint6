package com.example.banking.lib.errors;

import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;

public final class StatementErrors {
    private StatementErrors() {
    }

    public static ApiErrorException validation(String message, String field) {
        return new ApiErrorException(HttpStatus.BAD_REQUEST, "STATEMENT_VALIDATION_ERROR", message, field);
    }

    public static ApiErrorException accountNotFound(String field) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "STATEMENT_ACCOUNT_NOT_FOUND",
                "No account found for the provided identifier",
                field);
    }

    public static ApiErrorException notFound(String statementId) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "STATEMENT_NOT_FOUND",
                "No statement found for the provided identifier",
                statementId == null ? "statementId" : null);
    }

    public static ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "STATEMENT_FORBIDDEN",
                "Insufficient privileges to " + operation + " statements",
                null);
    }

    public static ApiErrorException conflict(String message, String field) {
        return new ApiErrorException(HttpStatus.CONFLICT, "STATEMENT_CONFLICT", message, field);
    }

    public static ApiErrorException dependencyFailure(String message) {
        return new ApiErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "STATEMENT_DEPENDENCY_FAILURE",
                message,
                null);
    }
}

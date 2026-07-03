package com.example.banking.lib.errors;

import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;

public final class AccountErrors {
    private AccountErrors() {
    }

    public static ApiErrorException validation(String message, String field) {
        return new ApiErrorException(HttpStatus.BAD_REQUEST, "ACCOUNT_VALIDATION_ERROR", message, field);
    }

    public static ApiErrorException notFound() {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "ACCOUNT_NOT_FOUND",
                "No account found with the provided accountId",
                "accountId");
    }

    public static ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_FORBIDDEN",
                "Insufficient privileges to " + operation + " account",
                null);
    }

    public static ApiErrorException conflict(String message, String field) {
        return new ApiErrorException(HttpStatus.CONFLICT, "ACCOUNT_CONFLICT", message, field);
    }

    public static ApiErrorException deleteBlocked() {
        return new ApiErrorException(
                HttpStatus.CONFLICT,
                "ACCOUNT_DELETE_BLOCKED",
                "Account deletion blocked by policy",
                null);
    }
}

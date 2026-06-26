package com.example.banking.lib.errors;

import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;

public final class StandingOrderErrors {
    private StandingOrderErrors() {
    }

    public static ApiErrorException validation(String message, String field) {
        return new ApiErrorException(HttpStatus.BAD_REQUEST, "STANDING_ORDER_VALIDATION_ERROR", message, field);
    }

    public static ApiErrorException notFound(String standingOrderId) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "STANDING_ORDER_NOT_FOUND",
                "No standing order found for the provided identifier",
                standingOrderId == null ? "standingOrderId" : null);
    }

    public static ApiErrorException accountNotFound(String field) {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "STANDING_ORDER_ACCOUNT_NOT_FOUND",
                "No account found for the provided identifier",
                field);
    }

    public static ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "STANDING_ORDER_FORBIDDEN",
                "Insufficient privileges to " + operation + " standing order",
                null);
    }

    public static ApiErrorException conflict(String message, String field) {
        return new ApiErrorException(HttpStatus.CONFLICT, "STANDING_ORDER_CONFLICT", message, field);
    }
}

package com.example.banking.lib.errors;

import org.springframework.http.HttpStatus;

import com.example.banking.api.common.ApiErrorException;

public final class CustomerErrors {
    private CustomerErrors() {
    }

    public static ApiErrorException validation(String message, String field) {
        return new ApiErrorException(HttpStatus.BAD_REQUEST, "CUSTOMER_VALIDATION_ERROR", message, field);
    }

    public static ApiErrorException notFound() {
        return new ApiErrorException(
                HttpStatus.NOT_FOUND,
                "CUSTOMER_NOT_FOUND",
                "No customer found with the provided customerId",
                "customerId");
    }

    public static ApiErrorException forbidden(String operation) {
        return new ApiErrorException(
                HttpStatus.FORBIDDEN,
                "CUSTOMER_FORBIDDEN",
                "Insufficient privileges to " + operation + " customer profile",
                null);
    }

    public static ApiErrorException conflict(String message, String field) {
        return new ApiErrorException(HttpStatus.CONFLICT, "CUSTOMER_CONFLICT", message, field);
    }

    public static ApiErrorException deleteBlocked() {
        return new ApiErrorException(
                HttpStatus.CONFLICT,
                "CUSTOMER_DELETE_BLOCKED",
                "Deletion blocked by policy",
                null);
    }
}

package com.example.banking.api.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiErrorExceptionTest {

    @Test
    void exposesConstructorFieldsThroughGetters() {
        ApiErrorException exception = new ApiErrorException(
                HttpStatus.BAD_REQUEST,
                "E_CODE",
                "Validation failed",
                "amount");

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("E_CODE", exception.getCode());
        assertEquals("Validation failed", exception.getMessage());
        assertEquals("amount", exception.getField());
    }
}

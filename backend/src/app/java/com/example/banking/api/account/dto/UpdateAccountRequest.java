package com.example.banking.api.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for update account request payload.")
public record UpdateAccountRequest(
        @Size(max = 64, message = "nickname must not exceed 64 characters")
        String nickname,

        @Pattern(regexp = "^(ACTIVE|SUSPENDED|CLOSED)$", message = "status must be ACTIVE, SUSPENDED, or CLOSED")
        String status,

        @DecimalMin(value = "0.00", message = "interestRate must be greater than or equal to 0")
        BigDecimal interestRate,

        @DecimalMin(value = "0.00", message = "balance must be greater than or equal to 0")
        BigDecimal balance) {
}

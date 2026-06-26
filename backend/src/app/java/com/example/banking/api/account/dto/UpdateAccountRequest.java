package com.example.banking.api.account.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(max = 64, message = "nickname must not exceed 64 characters")
        String nickname,

        @Pattern(regexp = "^(ACTIVE|SUSPENDED|CLOSED)$", message = "status must be ACTIVE, SUSPENDED, or CLOSED")
        String status) {
}

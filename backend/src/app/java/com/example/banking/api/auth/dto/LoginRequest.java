package com.example.banking.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String identity,
        @NotBlank String password) {
}

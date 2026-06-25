package com.example.banking.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank String identity) {
}

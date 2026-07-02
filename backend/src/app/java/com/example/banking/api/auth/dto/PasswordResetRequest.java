package com.example.banking.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for password reset request payload.")
public record PasswordResetRequest(@NotBlank String identity) {
}

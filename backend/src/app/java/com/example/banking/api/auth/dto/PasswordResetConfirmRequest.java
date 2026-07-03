package com.example.banking.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for password reset confirm request payload.")
public record PasswordResetConfirmRequest(
        @NotBlank @Email String identity,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(min = 8, max = 128) String passwordConfirmation) {
}

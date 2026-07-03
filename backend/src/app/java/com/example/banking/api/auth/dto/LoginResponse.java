package com.example.banking.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for login response payload.")
public record LoginResponse(String accessToken, String refreshToken, long expiresIn) {
}

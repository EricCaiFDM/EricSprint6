package com.example.banking.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAPI schema for refresh response payload.")
public record RefreshResponse(String accessToken, String refreshToken, long expiresIn) {
}

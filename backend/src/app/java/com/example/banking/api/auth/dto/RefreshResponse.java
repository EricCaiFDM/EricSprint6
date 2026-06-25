package com.example.banking.api.auth.dto;

public record RefreshResponse(String accessToken, String refreshToken, long expiresIn) {
}

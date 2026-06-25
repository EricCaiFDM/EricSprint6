package com.example.banking.api.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType) {
}

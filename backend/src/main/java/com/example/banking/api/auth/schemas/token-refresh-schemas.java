package com.example.banking.api.auth.schemas;

class RefreshRequestSchema {
    String refreshToken;
}

class RefreshResponseSchema {
    String accessToken;
    String refreshToken;
    int expiresIn;
}

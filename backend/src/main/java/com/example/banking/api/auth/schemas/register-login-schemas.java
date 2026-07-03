package com.example.banking.api.auth.schemas;

class RegisterRequest {
    String email;
    String password;
    String passwordConfirmation;
}

class LoginRequest {
    String identity;
    String password;
}

class LoginResponse {
    String accessToken;
    String refreshToken;
    int expiresIn;
}

package com.example.banking.lib.config;

class AuthConfig {
    String issuer = "banking-auth";
    int accessTokenSeconds = 900;
    int refreshTokenSeconds = 1209600;
    int passwordHashCost = 12;
}

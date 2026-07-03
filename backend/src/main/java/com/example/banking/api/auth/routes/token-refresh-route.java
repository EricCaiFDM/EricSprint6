package com.example.banking.api.auth.routes;

import java.util.Map;
import java.util.UUID;

class TokenRefreshRoute {
    TokenRefreshRoute() {}

    Map<String, Object> post(String refreshToken) {
        return Map.of(
                "accessToken", "acc-" + UUID.randomUUID(),
                "refreshToken", "ref-" + UUID.randomUUID(),
                "expiresIn", 900);
    }
}

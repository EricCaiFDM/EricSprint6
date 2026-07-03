package com.example.banking.api.auth.routes;

import java.util.Map;
import java.util.UUID;

class LoginRoute {
    LoginRoute() {}

    Map<String, Object> post(String identity, String password) {
        return Map.of(
                "accessToken", "acc-" + UUID.randomUUID(),
                "refreshToken", "ref-" + UUID.randomUUID(),
                "expiresIn", 900);
    }
}

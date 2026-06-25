package com.example.banking.api.auth.routes;

import java.util.Map;
import java.util.UUID;

class RegisterRoute {
    RegisterRoute() {}

    Map<String, Object> post(String email, String password, String confirmation) {
        return Map.of("status", "CREATED", "userId", UUID.randomUUID().toString());
    }
}

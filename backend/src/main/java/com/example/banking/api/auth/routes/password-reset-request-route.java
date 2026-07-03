package com.example.banking.api.auth.routes;

import java.util.Map;

class PasswordResetRequestRoute {
    PasswordResetRequestRoute() {}

    Map<String, Object> post(String identity) {
        return Map.of("status", "ACCEPTED", "message", "If the account exists, reset instructions will be sent.");
    }
}

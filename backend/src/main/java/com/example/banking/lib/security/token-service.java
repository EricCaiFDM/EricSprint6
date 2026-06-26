package com.example.banking.lib.security;

import java.time.Instant;
import java.util.UUID;

class TokenService {
    String issueAccessToken(UUID userId) {
        return "acc-" + userId + "-" + Instant.now().getEpochSecond();
    }

    String issueRefreshToken(UUID userId) {
        return "ref-" + userId + "-" + UUID.randomUUID();
    }
}

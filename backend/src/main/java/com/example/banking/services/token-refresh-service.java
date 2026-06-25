package com.example.banking.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class TokenRefreshService {
    private final AuthAuditService audit;
    private final Map<String, String> replacedTokenMap = new ConcurrentHashMap<>();

    TokenRefreshService(AuthAuditService audit) {
        this.audit = audit;
    }

    RefreshTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        if (replacedTokenMap.containsKey(refreshToken)) {
            audit.record("REFRESH_FAIL", null, "FAILURE", "E-004");
            throw new IllegalArgumentException("Refresh token replay detected");
        }

        UUID userId = UUID.randomUUID();
        String access = issueAccessToken(userId);
        String nextRefresh = issueRefreshToken(userId);
        replacedTokenMap.put(refreshToken, nextRefresh);
        audit.record("REFRESH_SUCCESS", userId, "SUCCESS", null);
        return new RefreshTokens(access, nextRefresh, 900);
    }

    void revokeOnReplay(String refreshToken) {
        replacedTokenMap.put(refreshToken, "REVOKED");
    }

    private String issueAccessToken(UUID userId) {
        return "acc-" + userId + "-" + (System.currentTimeMillis() / 1000L);
    }

    private String issueRefreshToken(UUID userId) {
        return "ref-" + userId + "-" + UUID.randomUUID();
    }

    static class RefreshTokens {
        private final String accessToken;
        private final String refreshToken;
        private final int expiresIn;

        RefreshTokens(String accessToken, String refreshToken, int expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }

        String getAccessToken() { return accessToken; }
        String getRefreshToken() { return refreshToken; }
        int getExpiresIn() { return expiresIn; }
    }
}

package com.example.banking.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

class LoginService {
    private final AuthRepository repository;
    private final AuthAuditService audit;

    LoginService(AuthRepository repository, AuthAuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    LoginTokens login(String identity, String password) {
        var user = repository.findByEmail(identity).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!"ACTIVE".equals(user.getStatus()) || !hash(password).equals(user.getPasswordHash())) {
            audit.record("LOGIN_FAIL", user.getId(), "FAILURE", "E-003");
            throw new IllegalArgumentException("Invalid credentials");
        }

        String access = issueAccessToken(user.getId());
        String refresh = issueRefreshToken(user.getId());
        audit.record("LOGIN_SUCCESS", user.getId(), "SUCCESS", null);
        return new LoginTokens(access, refresh, 900);
    }

    private String hash(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Hash algorithm unavailable", ex);
        }
    }

    private String issueAccessToken(UUID userId) {
        return "acc-" + userId + "-" + (System.currentTimeMillis() / 1000L);
    }

    private String issueRefreshToken(UUID userId) {
        return "ref-" + userId + "-" + UUID.randomUUID();
    }

    static class LoginTokens {
        private final String accessToken;
        private final String refreshToken;
        private final int expiresIn;

        LoginTokens(String accessToken, String refreshToken, int expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }

        String getAccessToken() { return accessToken; }
        String getRefreshToken() { return refreshToken; }
        int getExpiresIn() { return expiresIn; }
    }
}

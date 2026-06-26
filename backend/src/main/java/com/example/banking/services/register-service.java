package com.example.banking.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

class RegisterService {
    private final AuthRepository repository;
    private final AuthAuditService audit;

    RegisterService(AuthRepository repository, AuthAuditService audit) {
        this.repository = repository;
        this.audit = audit;
    }

    UUID register(String email, String password, String confirmation) {
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("Password confirmation mismatch");
        }
        repository.findByEmail(email).ifPresent(existing -> {
            throw new IllegalStateException("Duplicate identity");
        });
        UUID userId = UUID.randomUUID();
        repository.saveUser(new AuthRepository.UserAccountRecord(userId, email, hash(password), "ACTIVE"));
        audit.record("REGISTER_SUCCESS", userId, "SUCCESS", null);
        return userId;
    }

    private String hash(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Hash algorithm unavailable", ex);
        }
    }
}

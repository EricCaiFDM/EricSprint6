package com.example.banking.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AuthRepository;

@Service
public class AuthService {
    private final AuthRepository repository;

    public AuthService(AuthRepository repository) {
        this.repository = repository;
    }

    public UUID register(String email, String password, String passwordConfirmation) {
        if (password == null || !password.equals(passwordConfirmation)) {
            throw new IllegalArgumentException("Password confirmation mismatch");
        }
        if (repository.emailExists(email)) {
            throw new IllegalStateException("Email already registered");
        }

        UUID userId = UUID.randomUUID();
        repository.createUser(userId, email, hash(password));
        return userId;
    }

    public LoginTokens login(String identity, String password) {
        String passwordHash = repository.findPasswordHashByEmail(identity)
                .orElseThrow(() -> new SecurityException("Invalid credentials"));

        if (!passwordHash.equals(hash(password))) {
            throw new SecurityException("Invalid credentials");
        }

        return new LoginTokens(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Bearer");
    }

    private String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Hash algorithm unavailable", ex);
        }
    }

    public record LoginTokens(String accessToken, String refreshToken, String tokenType) {
    }
}

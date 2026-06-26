package com.example.banking.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AuthRepository;
import com.example.banking.lib.security.JwtTokenService;

@Service
public class AuthService {
    private final AuthRepository repository;
    private final JwtTokenService jwtTokenService;
    private final AuthenticationAuditService authenticationAuditService;

    public AuthService(
            AuthRepository repository,
            JwtTokenService jwtTokenService,
            AuthenticationAuditService authenticationAuditService) {
        this.repository = repository;
        this.jwtTokenService = jwtTokenService;
        this.authenticationAuditService = authenticationAuditService;
    }

    public UUID register(String email, String password, String passwordConfirmation) {
        if (password == null || !password.equals(passwordConfirmation)) {
            authenticationAuditService.record("REGISTER", normalizeIdentity(email), "FAILURE", "PASSWORD_MISMATCH");
            throw new IllegalArgumentException("Password confirmation mismatch");
        }
        if (repository.emailExists(email)) {
            authenticationAuditService.record("REGISTER", normalizeIdentity(email), "FAILURE", "DUPLICATE_IDENTITY");
            throw new IllegalStateException("Email already registered");
        }

        UUID userId = UUID.randomUUID();
        repository.createUser(userId, email, hash(password));
        authenticationAuditService.record("REGISTER", normalizeIdentity(email), "SUCCESS", null);
        return userId;
    }

    public LoginTokens login(String identity, String password) {
        AuthRepository.AuthUserCredentials credentials = repository.findCredentialsByEmail(identity)
                .orElseThrow(() -> {
                    authenticationAuditService.record("LOGIN", normalizeIdentity(identity), "FAILURE",
                            "INVALID_CREDENTIALS");
                    return new SecurityException("Invalid credentials or ineligible account state");
                });

        if (!credentials.passwordHash().equals(hash(password))) {
            authenticationAuditService.record("LOGIN", normalizeIdentity(identity), "FAILURE", "INVALID_CREDENTIALS");
            throw new SecurityException("Invalid credentials or ineligible account state");
        }

        if (!"ACTIVE".equalsIgnoreCase(credentials.accountStatus())) {
            authenticationAuditService.record("LOGIN", normalizeIdentity(identity), "FAILURE", "INELIGIBLE_ACCOUNT_STATE");
            throw new SecurityException("Invalid credentials or ineligible account state");
        }

        JwtTokenService.TokenPair tokenPair = jwtTokenService.issueTokens(
                credentials.userId(),
                credentials.email(),
                credentials.role());
        authenticationAuditService.record("LOGIN", normalizeIdentity(identity), "SUCCESS", null);

        return new LoginTokens(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                jwtTokenService.getAccessTokenExpiresInSeconds());
    }

    public void requestPasswordReset(String identity) {
        String normalizedIdentity = normalizeIdentity(identity);
        if (repository.emailExists(normalizedIdentity)) {
            authenticationAuditService.record("PASSWORD_RESET_REQUEST", normalizedIdentity, "SUCCESS", "ACCOUNT_FOUND");
            return;
        }

        authenticationAuditService.record("PASSWORD_RESET_REQUEST", normalizedIdentity, "SUCCESS", "ACCOUNT_NOT_FOUND");
    }

    public LoginTokens refreshAccessToken(String refreshToken) {
        JwtTokenService.RefreshTokenPrincipal refreshTokenPrincipal;
        try {
            refreshTokenPrincipal = jwtTokenService.validateRefreshToken(refreshToken);
        } catch (SecurityException exception) {
            authenticationAuditService.record("TOKEN_REFRESH", "unknown", "FAILURE", "INVALID_REFRESH_TOKEN");
            throw exception;
        }

        AuthRepository.AuthUserCredentials credentials = repository
                .findCredentialsByUserId(refreshTokenPrincipal.userId())
                .orElseThrow(() -> {
                    authenticationAuditService.record("TOKEN_REFRESH", refreshTokenPrincipal.email(), "FAILURE",
                            "UNKNOWN_IDENTITY");
                    return new SecurityException("Invalid or expired refresh token");
                });

        if (!"ACTIVE".equalsIgnoreCase(credentials.accountStatus())) {
            authenticationAuditService.record("TOKEN_REFRESH", refreshTokenPrincipal.email(), "FAILURE",
                    "INELIGIBLE_ACCOUNT_STATE");
            throw new SecurityException("Invalid or expired refresh token");
        }

        JwtTokenService.TokenPair rotatedTokenPair = jwtTokenService.issueTokens(
                credentials.userId(),
                credentials.email(),
                credentials.role());
        authenticationAuditService.record("TOKEN_REFRESH", refreshTokenPrincipal.email(), "SUCCESS", null);

        return new LoginTokens(
                rotatedTokenPair.accessToken(),
                rotatedTokenPair.refreshToken(),
                jwtTokenService.getAccessTokenExpiresInSeconds());
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

    private String normalizeIdentity(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
    }

    public record LoginTokens(String accessToken, String refreshToken, long expiresIn) {
    }
}

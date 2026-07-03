package com.example.banking.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AuthRepository;
import com.example.banking.lib.security.JwtTokenService;

@Service
public class AuthService {
    private static final Set<String> ALLOWED_ROLES = Set.of("CUSTOMER", "ADMIN");
    private static final String CLOSED_ACCOUNT_STATUS = "CLOSED";

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

    public UUID register(String email, String password, String passwordConfirmation, String role) {
        String normalizedIdentity = normalizeIdentity(email);
        if (password == null || !password.equals(passwordConfirmation)) {
            authenticationAuditService.record("REGISTER", normalizedIdentity, "FAILURE", "PASSWORD_MISMATCH");
            throw new IllegalArgumentException("Password confirmation mismatch");
        }

        String normalizedRole = normalizeRole(role);
        AuthRepository.AuthUserCredentials existingUser = repository.findCredentialsByEmail(normalizedIdentity).orElse(null);
        if (existingUser != null) {
            if (!CLOSED_ACCOUNT_STATUS.equalsIgnoreCase(existingUser.accountStatus())) {
                authenticationAuditService.record("REGISTER", normalizedIdentity, "FAILURE", "DUPLICATE_IDENTITY");
                throw new IllegalStateException("Email already registered with role " + existingUser.role());
            }

            boolean reactivated = repository.reactivateClosedIdentityByUserId(
                    existingUser.userId(),
                    hash(password),
                    normalizedRole);
            if (!reactivated) {
                authenticationAuditService.record("REGISTER", normalizedIdentity, "FAILURE", "IDENTITY_REACTIVATION_FAILED");
                throw new IllegalStateException("Unable to reactivate closed identity");
            }

            authenticationAuditService.record("REGISTER", normalizedIdentity, "SUCCESS", "IDENTITY_REACTIVATED");
            return parseUserId(existingUser.userId());
        }

        UUID userId = UUID.randomUUID();
        repository.createUser(userId, email, hash(password), normalizedRole);
        authenticationAuditService.record("REGISTER", normalizedIdentity, "SUCCESS", null);
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

    public void confirmPasswordReset(String identity, String password, String passwordConfirmation) {
        String normalizedIdentity = normalizeIdentity(identity);

        if (password == null || !password.equals(passwordConfirmation)) {
            authenticationAuditService.record("PASSWORD_RESET_CONFIRM", normalizedIdentity, "FAILURE", "PASSWORD_MISMATCH");
            throw new IllegalArgumentException("Password confirmation mismatch");
        }

        if (password.length() < 8 || password.length() > 128) {
            authenticationAuditService.record("PASSWORD_RESET_CONFIRM", normalizedIdentity, "FAILURE", "PASSWORD_POLICY_VIOLATION");
            throw new IllegalArgumentException("Password must be between 8 and 128 characters");
        }

        boolean updated = repository.updatePasswordHashByEmail(normalizedIdentity, hash(password));
        authenticationAuditService.record(
                "PASSWORD_RESET_CONFIRM",
                normalizedIdentity,
                "SUCCESS",
                updated ? "ACCOUNT_UPDATED" : "ACCOUNT_NOT_FOUND");
    }

    public boolean updateIdentityEmail(String userId, String email) {
        String normalizedUserId = userId == null ? "" : userId.trim();
        if (normalizedUserId.isEmpty()) {
            return false;
        }

        String normalizedIdentity = normalizeIdentity(email);
        if (normalizedIdentity.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        AuthRepository.AuthUserCredentials existingUser = repository.findCredentialsByEmail(normalizedIdentity)
                .orElse(null);
        if (existingUser != null && !normalizedUserId.equals(existingUser.userId())) {
            throw new IllegalStateException("Email already registered with role " + existingUser.role());
        }

        return repository.updateEmailByUserId(normalizedUserId, normalizedIdentity);
    }

    public boolean deactivateIdentity(String userId) {
        String normalizedUserId = userId == null ? "" : userId.trim();
        if (normalizedUserId.isEmpty()) {
            return false;
        }

        return repository.updateAccountStatusByUserId(normalizedUserId, CLOSED_ACCOUNT_STATUS);
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
            MessageDigest digest = createMessageDigest();
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Hash algorithm unavailable", ex);
        }
    }

    protected MessageDigest createMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    private String normalizeIdentity(String identity) {
        return identity == null ? "" : identity.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return "CUSTOMER";
        }

        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (ALLOWED_ROLES.contains(normalizedRole)) {
            return normalizedRole;
        }

        throw new IllegalArgumentException("Unsupported role. Allowed values: CUSTOMER, ADMIN");
    }

    private UUID parseUserId(String userId) {
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Stored user identifier is invalid", exception);
        }
    }

    public record LoginTokens(String accessToken, String refreshToken, long expiresIn) {
    }
}

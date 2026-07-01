package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.banking.lib.AuthRepository;
import com.example.banking.lib.security.JwtTokenService;

class AuthServiceTest {

    private FakeAuthRepository repository;
    private FakeJwtTokenService jwtTokenService;
    private FakeAuthenticationAuditService authenticationAuditService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repository = new FakeAuthRepository();
        jwtTokenService = new FakeJwtTokenService();
        authenticationAuditService = new FakeAuthenticationAuditService();
        service = new AuthService(repository, jwtTokenService, authenticationAuditService);
    }

    @Test
    void registerFailsWhenPasswordIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.register(" User@Example.com ", null, "password123", null));

        assertEquals("Password confirmation mismatch", exception.getMessage());
        assertEvent("REGISTER", "user@example.com", "FAILURE", "PASSWORD_MISMATCH");
        assertEquals(0, repository.createUserCalls);
    }

    @Test
    void registerFailsWhenPasswordConfirmationMismatches() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.register("User@Example.com", "password123", "different123", null));

        assertEquals("Password confirmation mismatch", exception.getMessage());
        assertEvent("REGISTER", "user@example.com", "FAILURE", "PASSWORD_MISMATCH");
        assertEquals(0, repository.createUserCalls);
    }

    @Test
    void registerFailsWhenIdentityAlreadyExists() {
        repository.findByEmailResult = Optional.of(credentials("user-1", "user@example.com", "hash", "ADMIN", "ACTIVE"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.register("user@example.com", "password123", "password123", "CUSTOMER"));

        assertEquals("Email already registered with role ADMIN", exception.getMessage());
        assertEvent("REGISTER", "user@example.com", "FAILURE", "DUPLICATE_IDENTITY");
        assertEquals(0, repository.createUserCalls);
    }

    @Test
    void registerUsesDefaultCustomerRoleWhenRoleIsNull() {
        UUID createdUserId = service.register("User@Example.com", "password123", "password123", null);

        assertNotNull(createdUserId);
        assertEquals(1, repository.createUserCalls);
        assertEquals(createdUserId, repository.createdUserId);
        assertEquals("User@Example.com", repository.createdEmail);
        assertEquals("CUSTOMER", repository.createdRole);
        assertEquals(sha256Base64("password123"), repository.createdPasswordHash);
        assertEvent("REGISTER", "user@example.com", "SUCCESS", null);
    }

    @Test
    void registerUsesDefaultCustomerRoleWhenRoleIsBlank() {
        UUID createdUserId = service.register("user@example.com", "password123", "password123", "   ");

        assertNotNull(createdUserId);
        assertEquals("CUSTOMER", repository.createdRole);
    }

    @Test
    void registerUsesNormalizedAdminRoleWhenProvided() {
        UUID createdUserId = service.register("user@example.com", "password123", "password123", " admin ");

        assertNotNull(createdUserId);
        assertEquals("ADMIN", repository.createdRole);
        assertEvent("REGISTER", "user@example.com", "SUCCESS", null);
    }

    @Test
    void registerFailsForUnsupportedRole() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.register("user@example.com", "password123", "password123", "MANAGER"));

        assertEquals("Unsupported role. Allowed values: CUSTOMER, ADMIN", exception.getMessage());
        assertEquals(0, repository.createUserCalls);
        assertEquals(0, authenticationAuditService.events.size());
    }

    @Test
    void registerThrowsWhenHashAlgorithmIsUnavailable() {
        AuthService brokenHashService = new BrokenHashAuthService(repository, jwtTokenService, authenticationAuditService);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> brokenHashService.register("user@example.com", "password123", "password123", "CUSTOMER"));

        assertEquals("Hash algorithm unavailable", exception.getMessage());
        assertTrue(exception.getCause() instanceof NoSuchAlgorithmException);
        assertEquals(0, repository.createUserCalls);
        assertEquals(0, authenticationAuditService.events.size());
    }

    @Test
    void loginFailsWhenIdentityIsUnknown() {
        repository.findByEmailResult = Optional.empty();

        SecurityException exception = assertThrows(SecurityException.class,
                () -> service.login("missing@example.com", "password123"));

        assertEquals("Invalid credentials or ineligible account state", exception.getMessage());
        assertEvent("LOGIN", "missing@example.com", "FAILURE", "INVALID_CREDENTIALS");
    }

    @Test
    void loginFailsWhenPasswordHashDoesNotMatch() {
        repository.findByEmailResult = Optional.of(credentials(
                "user-1",
                "user@example.com",
                sha256Base64("correct-password"),
                "CUSTOMER",
                "ACTIVE"));

        SecurityException exception = assertThrows(SecurityException.class,
                () -> service.login("user@example.com", "wrong-password"));

        assertEquals("Invalid credentials or ineligible account state", exception.getMessage());
        assertEvent("LOGIN", "user@example.com", "FAILURE", "INVALID_CREDENTIALS");
    }

    @Test
    void loginFailsWhenAccountStatusIsNotActive() {
        repository.findByEmailResult = Optional.of(credentials(
                "user-1",
                "user@example.com",
                sha256Base64("password123"),
                "CUSTOMER",
                "SUSPENDED"));

        SecurityException exception = assertThrows(SecurityException.class,
                () -> service.login("user@example.com", "password123"));

        assertEquals("Invalid credentials or ineligible account state", exception.getMessage());
        assertEvent("LOGIN", "user@example.com", "FAILURE", "INELIGIBLE_ACCOUNT_STATE");
    }

    @Test
    void loginSucceedsWhenCredentialsAndStateAreValid() {
        repository.findByEmailResult = Optional.of(credentials(
                "user-1",
                "user@example.com",
                sha256Base64("password123"),
                "CUSTOMER",
                "ACTIVE"));
        jwtTokenService.issueTokensResult = new JwtTokenService.TokenPair("access-token", "refresh-token");
        jwtTokenService.accessTokenExpiresInSeconds = 900L;

        AuthService.LoginTokens tokens = service.login("user@example.com", "password123");

        assertEquals("access-token", tokens.accessToken());
        assertEquals("refresh-token", tokens.refreshToken());
        assertEquals(900L, tokens.expiresIn());
        assertEquals("user-1", jwtTokenService.lastIssueUserId);
        assertEquals("user@example.com", jwtTokenService.lastIssueEmail);
        assertEquals("CUSTOMER", jwtTokenService.lastIssueRole);
        assertEvent("LOGIN", "user@example.com", "SUCCESS", null);
    }

    @Test
    void requestPasswordResetRecordsAccountFoundWhenIdentityExists() {
        repository.emailExistsResult = true;

        service.requestPasswordReset(" User@Example.com ");

        assertEquals("user@example.com", repository.lastEmailExistsIdentity);
        assertEvent("PASSWORD_RESET_REQUEST", "user@example.com", "SUCCESS", "ACCOUNT_FOUND");
    }

    @Test
    void requestPasswordResetRecordsAccountNotFoundWhenIdentityMissing() {
        repository.emailExistsResult = false;

        service.requestPasswordReset(null);

        assertEquals("", repository.lastEmailExistsIdentity);
        assertEvent("PASSWORD_RESET_REQUEST", "", "SUCCESS", "ACCOUNT_NOT_FOUND");
    }

    @Test
    void confirmPasswordResetFailsWhenPasswordIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.confirmPasswordReset(" User@Example.com ", null, "password123"));

        assertEquals("Password confirmation mismatch", exception.getMessage());
        assertEvent("PASSWORD_RESET_CONFIRM", "user@example.com", "FAILURE", "PASSWORD_MISMATCH");
    }

    @Test
    void confirmPasswordResetFailsWhenPasswordConfirmationMismatches() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.confirmPasswordReset("user@example.com", "password123", "different123"));

        assertEquals("Password confirmation mismatch", exception.getMessage());
        assertEvent("PASSWORD_RESET_CONFIRM", "user@example.com", "FAILURE", "PASSWORD_MISMATCH");
    }

    @Test
    void confirmPasswordResetFailsWhenPasswordIsTooShort() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.confirmPasswordReset("user@example.com", "short7", "short7"));

        assertEquals("Password must be between 8 and 128 characters", exception.getMessage());
        assertEvent("PASSWORD_RESET_CONFIRM", "user@example.com", "FAILURE", "PASSWORD_POLICY_VIOLATION");
    }

    @Test
    void confirmPasswordResetFailsWhenPasswordIsTooLong() {
        String longPassword = "a".repeat(129);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.confirmPasswordReset("user@example.com", longPassword, longPassword));

        assertEquals("Password must be between 8 and 128 characters", exception.getMessage());
        assertEvent("PASSWORD_RESET_CONFIRM", "user@example.com", "FAILURE", "PASSWORD_POLICY_VIOLATION");
    }

    @Test
    void confirmPasswordResetRecordsAccountUpdatedWhenRepositoryUpdatesPassword() {
        repository.updatePasswordResult = true;

        service.confirmPasswordReset(" User@Example.com ", "password123", "password123");

        assertEquals("user@example.com", repository.lastUpdateIdentity);
        assertEquals(sha256Base64("password123"), repository.lastUpdatePasswordHash);
        assertEvent("PASSWORD_RESET_CONFIRM", "user@example.com", "SUCCESS", "ACCOUNT_UPDATED");
    }

    @Test
    void confirmPasswordResetRecordsAccountNotFoundWhenRepositoryDoesNotUpdate() {
        repository.updatePasswordResult = false;

        service.confirmPasswordReset("user@example.com", "password123", "password123");

        assertEvent("PASSWORD_RESET_CONFIRM", "user@example.com", "SUCCESS", "ACCOUNT_NOT_FOUND");
    }

    @Test
    void refreshAccessTokenFailsWhenRefreshTokenIsInvalid() {
        jwtTokenService.validateException = new SecurityException("Invalid");

        SecurityException exception = assertThrows(SecurityException.class,
                () -> service.refreshAccessToken("invalid-refresh"));

        assertEquals("Invalid", exception.getMessage());
        assertEvent("TOKEN_REFRESH", "unknown", "FAILURE", "INVALID_REFRESH_TOKEN");
        assertEquals(null, repository.lastFindByUserId);
    }

    @Test
    void refreshAccessTokenFailsWhenRefreshPrincipalCannotBeResolvedToUser() {
        jwtTokenService.validatePrincipal = new JwtTokenService.RefreshTokenPrincipal("user-1", "user@example.com", "CUSTOMER");
        repository.findByUserIdResult = Optional.empty();

        SecurityException exception = assertThrows(SecurityException.class,
                () -> service.refreshAccessToken("valid-refresh"));

        assertEquals("Invalid or expired refresh token", exception.getMessage());
        assertEquals("user-1", repository.lastFindByUserId);
        assertEvent("TOKEN_REFRESH", "user@example.com", "FAILURE", "UNKNOWN_IDENTITY");
    }

    @Test
    void refreshAccessTokenFailsWhenUserAccountIsNotActive() {
        jwtTokenService.validatePrincipal = new JwtTokenService.RefreshTokenPrincipal("user-1", "user@example.com", "CUSTOMER");
        repository.findByUserIdResult = Optional.of(credentials(
                "user-1",
                "user@example.com",
                sha256Base64("password123"),
                "CUSTOMER",
                "SUSPENDED"));

        SecurityException exception = assertThrows(SecurityException.class,
                () -> service.refreshAccessToken("valid-refresh"));

        assertEquals("Invalid or expired refresh token", exception.getMessage());
        assertEvent("TOKEN_REFRESH", "user@example.com", "FAILURE", "INELIGIBLE_ACCOUNT_STATE");
    }

    @Test
    void refreshAccessTokenSucceedsAndReturnsRotatedTokens() {
        jwtTokenService.validatePrincipal = new JwtTokenService.RefreshTokenPrincipal("user-1", "user@example.com", "CUSTOMER");
        repository.findByUserIdResult = Optional.of(credentials(
                "user-1",
                "user@example.com",
                sha256Base64("password123"),
                "CUSTOMER",
                "ACTIVE"));
        jwtTokenService.issueTokensResult = new JwtTokenService.TokenPair("new-access", "new-refresh");
        jwtTokenService.accessTokenExpiresInSeconds = 900L;

        AuthService.LoginTokens tokens = service.refreshAccessToken("valid-refresh");

        assertEquals("new-access", tokens.accessToken());
        assertEquals("new-refresh", tokens.refreshToken());
        assertEquals(900L, tokens.expiresIn());
        assertEquals("user-1", jwtTokenService.lastIssueUserId);
        assertEquals("user@example.com", jwtTokenService.lastIssueEmail);
        assertEquals("CUSTOMER", jwtTokenService.lastIssueRole);
        assertEvent("TOKEN_REFRESH", "user@example.com", "SUCCESS", null);
    }

    private void assertEvent(String eventType, String identity, String outcome, String reasonCode) {
        assertTrue(!authenticationAuditService.events.isEmpty());
        FakeAuthenticationAuditService.AuditEvent event =
                authenticationAuditService.events.get(authenticationAuditService.events.size() - 1);
        assertEquals(eventType, event.eventType());
        assertEquals(identity, event.identity());
        assertEquals(outcome, event.outcome());
        assertEquals(reasonCode, event.reasonCode());
    }

    private static AuthRepository.AuthUserCredentials credentials(
            String userId,
            String email,
            String passwordHash,
            String role,
            String accountStatus) {
        return new AuthRepository.AuthUserCredentials(userId, email, passwordHash, role, accountStatus);
    }

    private static String sha256Base64(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class FakeAuthRepository extends AuthRepository {
        Optional<AuthUserCredentials> findByEmailResult = Optional.empty();
        Optional<AuthUserCredentials> findByUserIdResult = Optional.empty();
        boolean emailExistsResult;
        boolean updatePasswordResult;

        UUID createdUserId;
        String createdEmail;
        String createdPasswordHash;
        String createdRole;
        int createUserCalls;

        String lastFindByEmailIdentity;
        String lastFindByUserId;
        String lastEmailExistsIdentity;
        String lastUpdateIdentity;
        String lastUpdatePasswordHash;

        FakeAuthRepository() {
            super(null);
        }

        @Override
        public boolean emailExists(String email) {
            lastEmailExistsIdentity = email;
            return emailExistsResult;
        }

        @Override
        public void createUser(UUID userId, String email, String passwordHash, String role) {
            createUserCalls++;
            createdUserId = userId;
            createdEmail = email;
            createdPasswordHash = passwordHash;
            createdRole = role;
        }

        @Override
        public Optional<AuthUserCredentials> findCredentialsByEmail(String email) {
            lastFindByEmailIdentity = email;
            return findByEmailResult;
        }

        @Override
        public Optional<AuthUserCredentials> findCredentialsByUserId(String userId) {
            lastFindByUserId = userId;
            return findByUserIdResult;
        }

        @Override
        public boolean updatePasswordHashByEmail(String email, String passwordHash) {
            lastUpdateIdentity = email;
            lastUpdatePasswordHash = passwordHash;
            return updatePasswordResult;
        }
    }

    private static final class FakeJwtTokenService extends JwtTokenService {
        JwtTokenService.TokenPair issueTokensResult = new JwtTokenService.TokenPair("access", "refresh");
        JwtTokenService.RefreshTokenPrincipal validatePrincipal =
                new JwtTokenService.RefreshTokenPrincipal("user-1", "user@example.com", "CUSTOMER");
        SecurityException validateException;
        long accessTokenExpiresInSeconds = 900L;

        String lastIssueUserId;
        String lastIssueEmail;
        String lastIssueRole;

        FakeJwtTokenService() {
            super("issuer", "01234567890123456789012345678901", 15, 7);
        }

        @Override
        public JwtTokenService.TokenPair issueTokens(String userId, String email, String role) {
            lastIssueUserId = userId;
            lastIssueEmail = email;
            lastIssueRole = role;
            return issueTokensResult;
        }

        @Override
        public long getAccessTokenExpiresInSeconds() {
            return accessTokenExpiresInSeconds;
        }

        @Override
        public JwtTokenService.RefreshTokenPrincipal validateRefreshToken(String refreshToken) {
            if (validateException != null) {
                throw validateException;
            }
            return validatePrincipal;
        }
    }

    private static final class FakeAuthenticationAuditService extends AuthenticationAuditService {
        final List<AuditEvent> events = new ArrayList<>();

        FakeAuthenticationAuditService() {
            super(null);
        }

        @Override
        public void record(String eventType, String identity, String outcome, String reasonCode) {
            events.add(new AuditEvent(eventType, identity, outcome, reasonCode));
        }

        record AuditEvent(String eventType, String identity, String outcome, String reasonCode) {
        }
    }

    private static final class BrokenHashAuthService extends AuthService {
        BrokenHashAuthService(
                AuthRepository repository,
                JwtTokenService jwtTokenService,
                AuthenticationAuditService authenticationAuditService) {
            super(repository, jwtTokenService, authenticationAuditService);
        }

        @Override
        protected MessageDigest createMessageDigest() throws NoSuchAlgorithmException {
            throw new NoSuchAlgorithmException("SHA-256 unavailable");
        }
    }
}

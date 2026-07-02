package com.example.banking.lib;

import java.util.Optional;
import java.util.UUID;
import java.util.Locale;

import org.springframework.stereotype.Repository;

import com.example.banking.models.AuthUserEntity;

@Repository
public class AuthRepository {
    private static final String ACTIVE_ACCOUNT_STATUS = "ACTIVE";

    private final AuthUserJpaRepository authUserJpaRepository;

    public AuthRepository(AuthUserJpaRepository authUserJpaRepository) {
        this.authUserJpaRepository = authUserJpaRepository;
    }

    public boolean emailExists(String email) {
        return authUserJpaRepository.existsByEmail(normalizeEmail(email));
    }

    public void createUser(UUID userId, String email, String passwordHash, String role) {
        AuthUserEntity authUserEntity = new AuthUserEntity(
                userId.toString(),
                normalizeEmail(email),
                passwordHash,
                role,
                "ACTIVE");
        authUserJpaRepository.save(authUserEntity);
    }

    public Optional<AuthUserCredentials> findCredentialsByEmail(String email) {
        return authUserJpaRepository.findByEmail(normalizeEmail(email))
                .map(entity -> new AuthUserCredentials(
                        entity.getId(),
                        entity.getEmail(),
                        entity.getPasswordHash(),
                        entity.getRole(),
                        entity.getAccountStatus()));
    }

    public Optional<AuthUserCredentials> findCredentialsByUserId(String userId) {
                return authUserJpaRepository.findById(userId)
                    .map(entity -> new AuthUserCredentials(
                        entity.getId(),
                        entity.getEmail(),
                        entity.getPasswordHash(),
                        entity.getRole(),
                        entity.getAccountStatus()));
    }

    public boolean updatePasswordHashByEmail(String email, String passwordHash) {
        Optional<AuthUserEntity> candidate = authUserJpaRepository.findByEmail(normalizeEmail(email));
        if (candidate.isEmpty()) {
            return false;
        }

        AuthUserEntity authUserEntity = candidate.get();
        authUserEntity.setPasswordHash(passwordHash);
        authUserJpaRepository.save(authUserEntity);
        return true;
    }

    public boolean updateEmailByUserId(String userId, String email) {
        Optional<AuthUserEntity> candidate = authUserJpaRepository.findById(userId);
        if (candidate.isEmpty()) {
            return false;
        }

        AuthUserEntity authUserEntity = candidate.get();
        authUserEntity.setEmail(normalizeEmail(email));
        authUserJpaRepository.save(authUserEntity);
        return true;
    }

    public boolean updateAccountStatusByUserId(String userId, String accountStatus) {
        Optional<AuthUserEntity> candidate = authUserJpaRepository.findById(userId);
        if (candidate.isEmpty()) {
            return false;
        }

        AuthUserEntity authUserEntity = candidate.get();
        authUserEntity.setAccountStatus(normalizeStatus(accountStatus));
        authUserJpaRepository.save(authUserEntity);
        return true;
    }

    public boolean reactivateClosedIdentityByUserId(String userId, String passwordHash, String role) {
        Optional<AuthUserEntity> candidate = authUserJpaRepository.findById(userId);
        if (candidate.isEmpty()) {
            return false;
        }

        AuthUserEntity authUserEntity = candidate.get();
        authUserEntity.setPasswordHash(passwordHash);
        authUserEntity.setRole(normalizeRole(role));
        authUserEntity.setAccountStatus(ACTIVE_ACCOUNT_STATUS);
        authUserJpaRepository.save(authUserEntity);
        return true;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeStatus(String accountStatus) {
        return accountStatus == null ? "" : accountStatus.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }

    public record AuthUserCredentials(String userId, String email, String passwordHash, String role,
            String accountStatus) {
    }
}

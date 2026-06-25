package com.example.banking.lib;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.banking.models.AuthUserEntity;

@Repository
public class AuthRepository {
    private final AuthUserJpaRepository authUserJpaRepository;

    public AuthRepository(AuthUserJpaRepository authUserJpaRepository) {
        this.authUserJpaRepository = authUserJpaRepository;
    }

    public boolean emailExists(String email) {
        return authUserJpaRepository.existsByEmail(normalizeEmail(email));
    }

    public void createUser(UUID userId, String email, String passwordHash) {
        AuthUserEntity authUserEntity = new AuthUserEntity(
                userId.toString(),
                normalizeEmail(email),
                passwordHash,
                "CUSTOMER",
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

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public record AuthUserCredentials(String userId, String email, String passwordHash, String role,
            String accountStatus) {
    }
}

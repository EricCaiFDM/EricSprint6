package com.example.banking.services;

import java.util.Optional;
import java.util.UUID;

interface AuthRepository {
    Optional<UserAccountRecord> findByEmail(String email);
    Optional<UserAccountRecord> findById(UUID userId);
    void saveUser(UserAccountRecord user);
    void saveRefreshSession(RefreshSessionRecord session);

    class UserAccountRecord {
        private final UUID id;
        private final String email;
        private final String passwordHash;
        private final String status;

        public UserAccountRecord(UUID id, String email, String passwordHash, String status) {
            this.id = id;
            this.email = email;
            this.passwordHash = passwordHash;
            this.status = status;
        }

        public UUID getId() { return id; }
        public String getEmail() { return email; }
        public String getPasswordHash() { return passwordHash; }
        public String getStatus() { return status; }
    }

    class RefreshSessionRecord {
        private final UUID id;
        private final UUID userId;
        private final String tokenId;
        private final String tokenHash;
        private final String status;

        public RefreshSessionRecord(UUID id, UUID userId, String tokenId, String tokenHash, String status) {
            this.id = id;
            this.userId = userId;
            this.tokenId = tokenId;
            this.tokenHash = tokenHash;
            this.status = status;
        }

        public UUID getId() { return id; }
        public UUID getUserId() { return userId; }
        public String getTokenId() { return tokenId; }
        public String getTokenHash() { return tokenHash; }
        public String getStatus() { return status; }
    }
}

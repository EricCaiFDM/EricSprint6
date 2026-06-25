package com.example.banking.models;

import java.time.Instant;
import java.util.UUID;

class RefreshSession {
    UUID id;
    UUID userId;
    String tokenId;
    String tokenHash;
    Instant issuedAtUtc;
    Instant expiresAtUtc;
    Instant revokedAtUtc;
    String replacedByTokenId;
    String ipAddress;
    String userAgent;
}

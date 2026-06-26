package com.example.banking.models;

import java.time.Instant;
import java.util.UUID;

class PasswordResetRequest {
    UUID id;
    UUID userId;
    String requestIdentity;
    String requestTokenHash;
    Instant requestedAtUtc;
    Instant expiresAtUtc;
    String status;
}

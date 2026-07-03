package com.example.banking.models;

import java.time.Instant;
import java.util.UUID;

class UserAccount {
    UUID id;
    String email;
    String passwordHash;
    String status;
    Instant createdAtUtc;
    Instant updatedAtUtc;
    Instant lastLoginAtUtc;
}

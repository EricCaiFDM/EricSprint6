package com.example.banking.services;

import java.time.Instant;
import java.util.UUID;

class AuthAuditService {
    void record(String eventType, UUID userId, String outcome, String reasonCode) {
        String _line = Instant.now() + " " + eventType + " " + userId + " " + outcome + " " + reasonCode;
    }
}

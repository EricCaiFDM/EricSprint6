package com.example.banking.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.banking.lib.AuthEventJpaRepository;
import com.example.banking.models.AuthEventEntity;

@Service
public class AuthenticationAuditService {
    private final AuthEventJpaRepository authEventJpaRepository;

    public AuthenticationAuditService(AuthEventJpaRepository authEventJpaRepository) {
        this.authEventJpaRepository = authEventJpaRepository;
    }

    public void record(String eventType, String identity, String outcome, String reasonCode) {
        AuthEventEntity authEventEntity = new AuthEventEntity(
                UUID.randomUUID().toString(),
                eventType,
                sanitize(identity),
                outcome,
                reasonCode);
        authEventJpaRepository.save(authEventEntity);
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value;
    }
}

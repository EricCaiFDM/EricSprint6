package com.example.banking.models;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_events")
public class AuthEventEntity {
    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "identity", nullable = false, length = 255)
    private String identity;

    @Column(name = "outcome", nullable = false, length = 32)
    private String outcome;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuthEventEntity() {
    }

    public AuthEventEntity(String id, String eventType, String identity, String outcome, String reasonCode) {
        this.id = id;
        this.eventType = eventType;
        this.identity = identity;
        this.outcome = outcome;
        this.reasonCode = reasonCode;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getIdentity() {
        return identity;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

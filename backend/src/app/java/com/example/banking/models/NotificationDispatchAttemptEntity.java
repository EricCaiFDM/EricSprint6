package com.example.banking.models;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_dispatch_attempts")
public class NotificationDispatchAttemptEntity {
    @Id
    @Column(name = "attempt_id", nullable = false, length = 36)
    private String attemptId;

    @Column(name = "notification_event_id", nullable = false, length = 36)
    private String notificationEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "queued_at_utc", nullable = false)
    private Instant queuedAtUtc;

    @Column(name = "started_at_utc")
    private Instant startedAtUtc;

    @Column(name = "completed_at_utc")
    private Instant completedAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 48)
    private NotificationDispatchAttemptStatus status;

    @Column(name = "provider_reference_id", length = 128)
    private String providerReferenceId;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @PrePersist
    void onCreate() {
        if (attemptId == null) {
            attemptId = UUID.randomUUID().toString();
        }
        if (queuedAtUtc == null) {
            queuedAtUtc = Instant.now();
        }
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public String getNotificationEventId() {
        return notificationEventId;
    }

    public void setNotificationEventId(String notificationEventId) {
        this.notificationEventId = notificationEventId;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Instant getQueuedAtUtc() {
        return queuedAtUtc;
    }

    public void setQueuedAtUtc(Instant queuedAtUtc) {
        this.queuedAtUtc = queuedAtUtc;
    }

    public Instant getStartedAtUtc() {
        return startedAtUtc;
    }

    public void setStartedAtUtc(Instant startedAtUtc) {
        this.startedAtUtc = startedAtUtc;
    }

    public Instant getCompletedAtUtc() {
        return completedAtUtc;
    }

    public void setCompletedAtUtc(Instant completedAtUtc) {
        this.completedAtUtc = completedAtUtc;
    }

    public NotificationDispatchAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationDispatchAttemptStatus status) {
        this.status = status;
    }

    public String getProviderReferenceId() {
        return providerReferenceId;
    }

    public void setProviderReferenceId(String providerReferenceId) {
        this.providerReferenceId = providerReferenceId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }
}

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
@Table(name = "notification_preference_snapshots")
public class NotificationPreferenceSnapshotEntity {
    @Id
    @Column(name = "snapshot_id", nullable = false, length = 36)
    private String snapshotId;

    @Column(name = "notification_event_id", nullable = false, length = 36)
    private String notificationEventId;

    @Column(name = "recipient_id", nullable = false, length = 36)
    private String recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 16)
    private NotificationConsentStatus consentStatus;

    @Column(name = "allowed_channels", nullable = false, length = 160)
    private String allowedChannels;

    @Column(name = "restricted_channels", nullable = false, length = 160)
    private String restrictedChannels;

    @Column(name = "captured_at_utc", nullable = false)
    private Instant capturedAtUtc;

    @PrePersist
    void onCreate() {
        if (snapshotId == null) {
            snapshotId = UUID.randomUUID().toString();
        }
        if (capturedAtUtc == null) {
            capturedAtUtc = Instant.now();
        }
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getNotificationEventId() {
        return notificationEventId;
    }

    public void setNotificationEventId(String notificationEventId) {
        this.notificationEventId = notificationEventId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public NotificationConsentStatus getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(NotificationConsentStatus consentStatus) {
        this.consentStatus = consentStatus;
    }

    public String getAllowedChannels() {
        return allowedChannels;
    }

    public void setAllowedChannels(String allowedChannels) {
        this.allowedChannels = allowedChannels;
    }

    public String getRestrictedChannels() {
        return restrictedChannels;
    }

    public void setRestrictedChannels(String restrictedChannels) {
        this.restrictedChannels = restrictedChannels;
    }

    public Instant getCapturedAtUtc() {
        return capturedAtUtc;
    }

    public void setCapturedAtUtc(Instant capturedAtUtc) {
        this.capturedAtUtc = capturedAtUtc;
    }
}

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
@Table(name = "notification_delivery_outcomes")
public class NotificationDeliveryOutcomeEntity {
    @Id
    @Column(name = "outcome_id", nullable = false, length = 36)
    private String outcomeId;

    @Column(name = "notification_event_id", nullable = false, length = 36)
    private String notificationEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "final_status", nullable = false, length = 32)
    private NotificationDeliveryFinalStatus finalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivered_channel", length = 16)
    private NotificationChannel deliveredChannel;

    @Column(name = "completed_at_utc", nullable = false)
    private Instant completedAtUtc;

    @Column(name = "reason_code", length = 128)
    private String reasonCode;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    void onCreate() {
        if (outcomeId == null) {
            outcomeId = UUID.randomUUID().toString();
        }
        if (completedAtUtc == null) {
            completedAtUtc = Instant.now();
        }
    }

    public String getOutcomeId() {
        return outcomeId;
    }

    public void setOutcomeId(String outcomeId) {
        this.outcomeId = outcomeId;
    }

    public String getNotificationEventId() {
        return notificationEventId;
    }

    public void setNotificationEventId(String notificationEventId) {
        this.notificationEventId = notificationEventId;
    }

    public NotificationDeliveryFinalStatus getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(NotificationDeliveryFinalStatus finalStatus) {
        this.finalStatus = finalStatus;
    }

    public NotificationChannel getDeliveredChannel() {
        return deliveredChannel;
    }

    public void setDeliveredChannel(NotificationChannel deliveredChannel) {
        this.deliveredChannel = deliveredChannel;
    }

    public Instant getCompletedAtUtc() {
        return completedAtUtc;
    }

    public void setCompletedAtUtc(Instant completedAtUtc) {
        this.completedAtUtc = completedAtUtc;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}

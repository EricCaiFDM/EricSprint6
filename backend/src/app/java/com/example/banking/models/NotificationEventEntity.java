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
@Table(name = "notification_events")
public class NotificationEventEntity {
    @Id
    @Column(name = "notification_event_id", nullable = false, length = 36)
    private String notificationEventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_scope_type", nullable = false, length = 16)
    private NotificationRecipientScopeType recipientScopeType;

    @Column(name = "recipient_scope_id", nullable = false, length = 36)
    private String recipientScopeId;

    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    @Column(name = "template_context", nullable = false, columnDefinition = "TEXT")
    private String templateContext;

    @Column(name = "triggered_at_utc", nullable = false)
    private Instant triggeredAtUtc;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationEventStatus status;

    @Column(name = "correlation_id", length = 36)
    private String correlationId;

    @Column(name = "completed_at_utc")
    private Instant completedAtUtc;

    @PrePersist
    void onCreate() {
        if (notificationEventId == null) {
            notificationEventId = UUID.randomUUID().toString();
        }
        if (triggeredAtUtc == null) {
            triggeredAtUtc = Instant.now();
        }
        if (status == null) {
            status = NotificationEventStatus.PENDING;
        }
    }

    public String getNotificationEventId() {
        return notificationEventId;
    }

    public void setNotificationEventId(String notificationEventId) {
        this.notificationEventId = notificationEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public NotificationRecipientScopeType getRecipientScopeType() {
        return recipientScopeType;
    }

    public void setRecipientScopeType(NotificationRecipientScopeType recipientScopeType) {
        this.recipientScopeType = recipientScopeType;
    }

    public String getRecipientScopeId() {
        return recipientScopeId;
    }

    public void setRecipientScopeId(String recipientScopeId) {
        this.recipientScopeId = recipientScopeId;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateContext() {
        return templateContext;
    }

    public void setTemplateContext(String templateContext) {
        this.templateContext = templateContext;
    }

    public Instant getTriggeredAtUtc() {
        return triggeredAtUtc;
    }

    public void setTriggeredAtUtc(Instant triggeredAtUtc) {
        this.triggeredAtUtc = triggeredAtUtc;
    }

    public NotificationEventStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationEventStatus status) {
        this.status = status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getCompletedAtUtc() {
        return completedAtUtc;
    }

    public void setCompletedAtUtc(Instant completedAtUtc) {
        this.completedAtUtc = completedAtUtc;
    }
}

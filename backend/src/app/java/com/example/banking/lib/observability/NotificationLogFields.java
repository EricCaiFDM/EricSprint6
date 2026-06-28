package com.example.banking.lib.observability;

public final class NotificationLogFields {
    private NotificationLogFields() {
    }

    public static final String NOTIFICATION_EVENT_ID = "notificationEventId";
    public static final String EVENT_TYPE = "eventType";
    public static final String RECIPIENT_SCOPE_TYPE = "recipientScopeType";
    public static final String RECIPIENT_SCOPE_ID = "recipientScopeId";
    public static final String CHANNEL = "channel";
    public static final String ATTEMPT_NUMBER = "attemptNumber";
    public static final String ATTEMPT_STATUS = "attemptStatus";
    public static final String FINAL_STATUS = "finalStatus";
    public static final String REASON_CODE = "reasonCode";
}

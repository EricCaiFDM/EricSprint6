package com.example.banking.models;

import java.util.List;

public record NotificationPreferenceSnapshot(
        NotificationConsentStatus consentStatus,
        List<NotificationChannel> allowedChannels,
        List<NotificationChannel> restrictedChannels) {
}

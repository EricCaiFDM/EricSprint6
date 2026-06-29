package com.example.banking.services;

import org.springframework.stereotype.Service;

import com.example.banking.models.NotificationChannel;
import com.example.banking.models.NotificationConsentStatus;
import com.example.banking.models.NotificationPreferenceSnapshot;

@Service
public class NotificationPreferenceEnforcementService {
    public EnforcementDecision evaluate(NotificationPreferenceSnapshot snapshot, NotificationChannel channel) {
        if (snapshot.consentStatus() == NotificationConsentStatus.RESTRICTED) {
            return new EnforcementDecision(true, "CONSENT_RESTRICTED");
        }

        if (snapshot.restrictedChannels() != null && snapshot.restrictedChannels().contains(channel)) {
            return new EnforcementDecision(true, "CHANNEL_RESTRICTED");
        }

        return new EnforcementDecision(false, null);
    }

    public record EnforcementDecision(boolean blocked, String reasonCode) {
    }
}

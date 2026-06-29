package com.example.banking.lib.security;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.banking.models.NotificationChannel;
import com.example.banking.models.NotificationConsentStatus;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationPreferenceSnapshot;

@Service
public class NotificationPreferencePolicy {
    public NotificationPreferenceSnapshot evaluate(NotificationEventEntity event, Map<String, Object> templateContext) {
        NotificationConsentStatus consentStatus = extractConsentStatus(templateContext);
        List<NotificationChannel> restrictedChannels = extractRestrictedChannels(templateContext);

        if (consentStatus == NotificationConsentStatus.RESTRICTED) {
            return new NotificationPreferenceSnapshot(
                    consentStatus,
                    List.of(),
                    List.of(NotificationChannel.values()));
        }

        List<NotificationChannel> allowedChannels = Arrays.stream(NotificationChannel.values())
                .filter(channel -> !restrictedChannels.contains(channel))
                .toList();

        return new NotificationPreferenceSnapshot(consentStatus, allowedChannels, restrictedChannels);
    }

    private NotificationConsentStatus extractConsentStatus(Map<String, Object> templateContext) {
        if (templateContext == null) {
            return NotificationConsentStatus.CONSENTED;
        }

        Object value = templateContext.get("consentStatus");
        if (value == null) {
            return NotificationConsentStatus.CONSENTED;
        }

        String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
        if ("RESTRICTED".equals(normalized)) {
            return NotificationConsentStatus.RESTRICTED;
        }
        return NotificationConsentStatus.CONSENTED;
    }

    private List<NotificationChannel> extractRestrictedChannels(Map<String, Object> templateContext) {
        if (templateContext == null || !templateContext.containsKey("restrictedChannels")) {
            return List.of();
        }

        Object value = templateContext.get("restrictedChannels");
        if (value instanceof List<?> listValue) {
            return listValue.stream()
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .map(item -> item.toUpperCase(Locale.ROOT))
                    .map(NotificationChannel::valueOf)
                    .distinct()
                    .toList();
        }

        if (value instanceof String stringValue) {
            return Arrays.stream(stringValue.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .map(item -> item.toUpperCase(Locale.ROOT))
                    .map(NotificationChannel::valueOf)
                    .distinct()
                    .toList();
        }

        return List.of();
    }
}

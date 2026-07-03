package com.example.banking.services;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.banking.models.NotificationChannel;
import com.example.banking.models.NotificationDispatchAttemptStatus;

@Component
public class DefaultChannelDispatchAdapter implements ChannelDispatchAdapter {
    @Override
    public ChannelDispatchResult dispatch(
            NotificationChannel channel,
            String templateCode,
            String sanitizedTemplateContext,
            String notificationEventId,
            int attemptNumber) {
        String normalizedTemplate = templateCode == null ? "" : templateCode.trim().toUpperCase(Locale.ROOT);

        if (normalizedTemplate.isBlank() || normalizedTemplate.contains("INVALID")) {
            return new ChannelDispatchResult(
                    NotificationDispatchAttemptStatus.FAILED_TEMPLATE_RESOLUTION,
                    "TEMPLATE_RESOLUTION_FAILED",
                    null,
                    false,
                    false);
        }

        String normalizedContext = sanitizedTemplateContext == null ? "" : sanitizedTemplateContext;
        if (normalizedContext.contains("\"simulateChannelUnavailable\":true") && channel == NotificationChannel.EMAIL) {
            return new ChannelDispatchResult(
                    NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE,
                    "CHANNEL_UNAVAILABLE",
                    null,
                    true,
                    false);
        }

        if (normalizedContext.contains("\"simulatePushUnavailable\":true") && channel == NotificationChannel.PUSH) {
            return new ChannelDispatchResult(
                    NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE,
                    "CHANNEL_UNAVAILABLE",
                    null,
                    true,
                    false);
        }

        return new ChannelDispatchResult(
                NotificationDispatchAttemptStatus.SUCCEEDED,
                null,
                "provider-" + UUID.randomUUID(),
                false,
                true);
    }
}

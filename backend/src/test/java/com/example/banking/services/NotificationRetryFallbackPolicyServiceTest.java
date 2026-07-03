package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.banking.lib.config.NotificationModuleConfig;
import com.example.banking.models.NotificationChannel;
import com.example.banking.models.NotificationConsentStatus;
import com.example.banking.models.NotificationDispatchAttemptStatus;
import com.example.banking.models.NotificationPreferenceSnapshot;

class NotificationRetryFallbackPolicyServiceTest {

    @Test
    void resolveDispatchOrderHandlesEmptyAndConfiguredFallbackOrdering() {
        NotificationRetryFallbackPolicyService service = new NotificationRetryFallbackPolicyService(config("EMAIL,SMS,PUSH", 2, 30, "STANDARD"));

        NotificationPreferenceSnapshot nullAllowed = new NotificationPreferenceSnapshot(
                NotificationConsentStatus.CONSENTED,
                null,
                List.of());
        NotificationPreferenceSnapshot emptyAllowed = new NotificationPreferenceSnapshot(
                NotificationConsentStatus.CONSENTED,
                List.of(),
                List.of());

        assertEquals(List.of(), service.resolveDispatchOrder(nullAllowed));
        assertEquals(List.of(), service.resolveDispatchOrder(emptyAllowed));

        NotificationPreferenceSnapshot ordered = new NotificationPreferenceSnapshot(
                NotificationConsentStatus.CONSENTED,
                List.of(NotificationChannel.PUSH, NotificationChannel.EMAIL),
                List.of());
        assertEquals(
                List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH),
                service.resolveDispatchOrder(ordered));

        NotificationPreferenceSnapshot includesRemaining = new NotificationPreferenceSnapshot(
                NotificationConsentStatus.CONSENTED,
                List.of(NotificationChannel.IN_APP, NotificationChannel.SMS),
                List.of());
        assertEquals(
                List.of(NotificationChannel.SMS, NotificationChannel.IN_APP),
                service.resolveDispatchOrder(includesRemaining));
    }

    @Test
    void shouldRetryUsesStatusAndConfiguredAttemptThreshold() {
        NotificationRetryFallbackPolicyService service = new NotificationRetryFallbackPolicyService(config("EMAIL,SMS", 2, 30, "STANDARD"));

        assertFalse(service.shouldRetry(1, NotificationDispatchAttemptStatus.SUCCEEDED));
        assertFalse(service.shouldRetry(1, NotificationDispatchAttemptStatus.FAILED_TEMPLATE_RESOLUTION));
        assertTrue(service.shouldRetry(1, NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE));
        assertFalse(service.shouldRetry(2, NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE));

        NotificationRetryFallbackPolicyService minAttemptsService = new NotificationRetryFallbackPolicyService(config("EMAIL,SMS", 0, 30, "STANDARD"));
        assertTrue(minAttemptsService.shouldRetry(0, NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE));
        assertFalse(minAttemptsService.shouldRetry(1, NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE));
    }

    @Test
    void nextRetryAtAndDefaultPolicyCodeUseConfiguration() {
        NotificationRetryFallbackPolicyService service = new NotificationRetryFallbackPolicyService(config("EMAIL,SMS", 2, 15, " custom_policy "));

        Instant reference = Instant.parse("2026-06-30T10:15:30Z");
        assertEquals(Instant.parse("2026-06-30T10:15:45Z"), service.nextRetryAt(reference));

        Instant before = Instant.now();
        Instant computed = service.nextRetryAt(null);
        Instant after = Instant.now();
        assertTrue(!computed.isBefore(before.plusSeconds(15)));
        assertTrue(!computed.isAfter(after.plusSeconds(16)));

        assertEquals("CUSTOM_POLICY", service.defaultPolicyCode());
    }

    private NotificationModuleConfig config(String fallbackOrder, int maxAttempts, int retryDelaySeconds, String defaultPolicyCode) {
        NotificationModuleConfig config = new NotificationModuleConfig();
        config.setFallbackOrder(fallbackOrder);
        config.setMaxRetryAttempts(maxAttempts);
        config.setRetryDelaySeconds(retryDelaySeconds);
        config.setDefaultPolicyCode(defaultPolicyCode);
        return config;
    }
}
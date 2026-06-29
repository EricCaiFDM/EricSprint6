package com.example.banking.lib.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.banking.models.NotificationChannel;
import com.example.banking.models.NotificationConsentStatus;
import com.example.banking.models.NotificationEventEntity;
import com.example.banking.models.NotificationPreferenceSnapshot;

class NotificationPreferencePolicyTest {

    private final NotificationPreferencePolicy policy = new NotificationPreferencePolicy();

    @Test
    void defaultsToConsentedWhenTemplateContextMissing() {
        NotificationPreferenceSnapshot snapshot = policy.evaluate(new NotificationEventEntity(), null);

        assertEquals(NotificationConsentStatus.CONSENTED, snapshot.consentStatus());
        assertEquals(List.of(), snapshot.restrictedChannels());
        assertEquals(List.of(NotificationChannel.values()).size(), snapshot.allowedChannels().size());
    }

    @Test
    void restrictedConsentBlocksAllChannels() {
        NotificationPreferenceSnapshot snapshot = policy.evaluate(
                new NotificationEventEntity(),
                Map.of("consentStatus", "restricted"));

        assertEquals(NotificationConsentStatus.RESTRICTED, snapshot.consentStatus());
        assertTrue(snapshot.allowedChannels().isEmpty());
        assertEquals(List.of(NotificationChannel.values()).size(), snapshot.restrictedChannels().size());
    }

    @Test
    void restrictedChannelsFromListAreExcludedFromAllowedChannels() {
        NotificationPreferenceSnapshot snapshot = policy.evaluate(
                new NotificationEventEntity(),
                Map.of("restrictedChannels", List.of("sms", "in_app")));

        assertEquals(NotificationConsentStatus.CONSENTED, snapshot.consentStatus());
        assertTrue(snapshot.restrictedChannels().contains(NotificationChannel.SMS));
        assertTrue(snapshot.restrictedChannels().contains(NotificationChannel.IN_APP));
        assertTrue(snapshot.allowedChannels().stream().noneMatch(channel -> channel == NotificationChannel.SMS));
        assertTrue(snapshot.allowedChannels().stream().noneMatch(channel -> channel == NotificationChannel.IN_APP));
    }

    @Test
    void restrictedChannelsFromStringAreParsedAndDeduplicated() {
        NotificationPreferenceSnapshot snapshot = policy.evaluate(
                new NotificationEventEntity(),
                Map.of("restrictedChannels", "email, sms, email"));

        assertEquals(2, snapshot.restrictedChannels().size());
        assertTrue(snapshot.restrictedChannels().contains(NotificationChannel.EMAIL));
        assertTrue(snapshot.restrictedChannels().contains(NotificationChannel.SMS));
        assertTrue(snapshot.allowedChannels().stream().noneMatch(channel -> channel == NotificationChannel.EMAIL));
        assertTrue(snapshot.allowedChannels().stream().noneMatch(channel -> channel == NotificationChannel.SMS));
    }

    @Test
    void nonListAndNonStringRestrictedChannelsFallsBackToNoRestrictions() {
        NotificationPreferenceSnapshot snapshot = policy.evaluate(
                new NotificationEventEntity(),
                Map.of("restrictedChannels", 42));

        assertEquals(List.of(), snapshot.restrictedChannels());
        assertEquals(List.of(NotificationChannel.values()).size(), snapshot.allowedChannels().size());
    }
}

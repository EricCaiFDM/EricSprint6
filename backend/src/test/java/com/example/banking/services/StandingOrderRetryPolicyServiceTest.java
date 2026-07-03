package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.banking.lib.config.StandingOrderModuleConfig;

class StandingOrderRetryPolicyServiceTest {

    private StandingOrderRetryPolicyService service;

    @BeforeEach
    void setUp() {
        StandingOrderModuleConfig config = new StandingOrderModuleConfig();
        config.setDefaultRetryPolicyCode("standard");
        config.setMaxRetryAttempts(3);
        config.setRetryDelayMinutes(30);
        service = new StandingOrderRetryPolicyService(config);
    }

    @Test
    void returnsTerminalDecisionWhenAttemptLimitReached() {
        var decision = service.evaluateRetry("STANDARD", "GENERIC_FAILURE", 3, Instant.parse("2026-06-29T10:00:00Z"));

        assertFalse(decision.scheduleRetry());
        assertEquals("MAX_ATTEMPTS_REACHED", decision.decisionCode());
        assertEquals("STANDARD", decision.policyCode());
    }

    @Test
    void returnsTerminalDecisionForIneligibleAccountFailures() {
        var decision = service.evaluateRetry("STANDARD", "FAILED_INELIGIBLE_ACCOUNT", 1, Instant.parse("2026-06-29T10:00:00Z"));

        assertFalse(decision.scheduleRetry());
        assertEquals("INELIGIBLE_ACCOUNT_TERMINAL", decision.decisionCode());
    }

    @Test
    void returnsPolicyDisabledDecisionWhenNoRetryPolicySelected() {
        var decision = service.evaluateRetry("no_retry", "GENERIC_FAILURE", 1, Instant.parse("2026-06-29T10:00:00Z"));

        assertFalse(decision.scheduleRetry());
        assertEquals("POLICY_DISABLED", decision.decisionCode());
        assertEquals("NO_RETRY", decision.policyCode());
    }

    @Test
    void schedulesRetryUsingDefaultPolicyWhenCodeMissing() {
        Instant now = Instant.parse("2026-06-29T10:00:00Z");

        var decision = service.evaluateRetry(null, "GENERIC_FAILURE", 1, now);

        assertTrue(decision.scheduleRetry());
        assertEquals("RETRY_SCHEDULED", decision.decisionCode());
        assertEquals("STANDARD", decision.policyCode());
        assertNotNull(decision.nextRetryAtUtc());
        assertEquals(Instant.parse("2026-06-29T10:30:00Z"), decision.nextRetryAtUtc());
    }

    @Test
    void usesCurrentInstantWhenNowIsNull() {
        var decision = service.evaluateRetry("STANDARD", "GENERIC_FAILURE", 1, null);

        assertTrue(decision.scheduleRetry());
        assertNotNull(decision.nextRetryAtUtc());
    }
}

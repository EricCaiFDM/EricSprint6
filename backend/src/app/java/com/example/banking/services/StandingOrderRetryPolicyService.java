package com.example.banking.services;

import java.time.Instant;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.example.banking.lib.config.StandingOrderModuleConfig;

@Service
public class StandingOrderRetryPolicyService {
    private final StandingOrderModuleConfig standingOrderModuleConfig;

    public StandingOrderRetryPolicyService(StandingOrderModuleConfig standingOrderModuleConfig) {
        this.standingOrderModuleConfig = standingOrderModuleConfig;
    }

    public RetryDecision evaluateRetry(
            String retryPolicyCode,
            String failureReasonCode,
            int attemptNumber,
            Instant nowUtc) {
        String normalizedCode = normalizePolicyCode(retryPolicyCode);
        Instant evaluatedAt = nowUtc == null ? Instant.now() : nowUtc;

        if (attemptNumber >= Math.max(1, standingOrderModuleConfig.getMaxRetryAttempts())) {
            return new RetryDecision(false, null, "MAX_ATTEMPTS_REACHED", normalizedCode);
        }

        if ("FAILED_INELIGIBLE_ACCOUNT".equals(failureReasonCode)) {
            return new RetryDecision(false, null, "INELIGIBLE_ACCOUNT_TERMINAL", normalizedCode);
        }

        if ("NO_RETRY".equals(normalizedCode)) {
            return new RetryDecision(false, null, "POLICY_DISABLED", normalizedCode);
        }

        Instant nextRetryAt = evaluatedAt.plus(standingOrderModuleConfig.retryDelay());
        return new RetryDecision(true, nextRetryAt, "RETRY_SCHEDULED", normalizedCode);
    }

    private String normalizePolicyCode(String retryPolicyCode) {
        if (retryPolicyCode == null || retryPolicyCode.isBlank()) {
            return standingOrderModuleConfig.getDefaultRetryPolicyCode();
        }
        return retryPolicyCode.trim().toUpperCase(Locale.ROOT);
    }

    public record RetryDecision(
            boolean scheduleRetry,
            Instant nextRetryAtUtc,
            String decisionCode,
            String policyCode) {
    }
}

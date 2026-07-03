package com.example.banking.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.banking.lib.config.NotificationModuleConfig;
import com.example.banking.models.NotificationChannel;
import com.example.banking.models.NotificationDispatchAttemptStatus;
import com.example.banking.models.NotificationPreferenceSnapshot;

@Service
public class NotificationRetryFallbackPolicyService {
    private final NotificationModuleConfig config;

    public NotificationRetryFallbackPolicyService(NotificationModuleConfig config) {
        this.config = config;
    }

    public List<NotificationChannel> resolveDispatchOrder(NotificationPreferenceSnapshot snapshot) {
        List<NotificationChannel> allowed = snapshot.allowedChannels();
        if (allowed == null || allowed.isEmpty()) {
            return List.of();
        }

        List<NotificationChannel> ordered = new ArrayList<>();
        for (NotificationChannel configured : config.fallbackChannels()) {
            if (allowed.contains(configured)) {
                ordered.add(configured);
            }
        }
        for (NotificationChannel channel : allowed) {
            if (!ordered.contains(channel)) {
                ordered.add(channel);
            }
        }
        return ordered;
    }

    public boolean shouldRetry(int attemptNumber, NotificationDispatchAttemptStatus status) {
        if (status != NotificationDispatchAttemptStatus.FAILED_CHANNEL_UNAVAILABLE) {
            return false;
        }
        return attemptNumber < Math.max(1, config.getMaxRetryAttempts());
    }

    public Instant nextRetryAt(Instant from) {
        Instant reference = from == null ? Instant.now() : from;
        return reference.plus(config.retryDelay());
    }

    public String defaultPolicyCode() {
        return config.getDefaultPolicyCode();
    }
}

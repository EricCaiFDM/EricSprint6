package com.example.banking.lib.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.example.banking.models.NotificationChannel;

@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationModuleConfig {
    private int maxPageSize = 100;
    private int maxRetryAttempts = 2;
    private int retryDelaySeconds = 30;
    private String defaultPolicyCode = "STANDARD";
    private String fallbackOrder = "EMAIL,SMS,PUSH,IN_APP";

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public String getDefaultPolicyCode() {
        return defaultPolicyCode;
    }

    public void setDefaultPolicyCode(String defaultPolicyCode) {
        if (defaultPolicyCode == null || defaultPolicyCode.isBlank()) {
            this.defaultPolicyCode = "STANDARD";
            return;
        }
        this.defaultPolicyCode = defaultPolicyCode.trim().toUpperCase();
    }

    public String getFallbackOrder() {
        return fallbackOrder;
    }

    public void setFallbackOrder(String fallbackOrder) {
        if (fallbackOrder == null || fallbackOrder.isBlank()) {
            this.fallbackOrder = "EMAIL,SMS,PUSH,IN_APP";
            return;
        }
        this.fallbackOrder = fallbackOrder;
    }

    public Duration retryDelay() {
        return Duration.ofSeconds(Math.max(1, retryDelaySeconds));
    }

    public List<NotificationChannel> fallbackChannels() {
        return Arrays.stream(fallbackOrder.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toUpperCase)
                .map(NotificationChannel::valueOf)
                .toList();
    }
}

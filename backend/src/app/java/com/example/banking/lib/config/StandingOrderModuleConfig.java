package com.example.banking.lib.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "standing-order")
public class StandingOrderModuleConfig {
    private int maxPageSize = 100;
    private int schedulerWindowMinutes = 5;
    private int maxRetryAttempts = 3;
    private int retryDelayMinutes = 30;
    private String defaultRetryPolicyCode = "STANDARD";

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public int getSchedulerWindowMinutes() {
        return schedulerWindowMinutes;
    }

    public void setSchedulerWindowMinutes(int schedulerWindowMinutes) {
        this.schedulerWindowMinutes = schedulerWindowMinutes;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public int getRetryDelayMinutes() {
        return retryDelayMinutes;
    }

    public void setRetryDelayMinutes(int retryDelayMinutes) {
        this.retryDelayMinutes = retryDelayMinutes;
    }

    public String getDefaultRetryPolicyCode() {
        return defaultRetryPolicyCode;
    }

    public void setDefaultRetryPolicyCode(String defaultRetryPolicyCode) {
        if (defaultRetryPolicyCode == null || defaultRetryPolicyCode.isBlank()) {
            this.defaultRetryPolicyCode = "STANDARD";
            return;
        }
        this.defaultRetryPolicyCode = defaultRetryPolicyCode.trim().toUpperCase();
    }

    public Duration retryDelay() {
        return Duration.ofMinutes(Math.max(1, retryDelayMinutes));
    }
}

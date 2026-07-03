package com.example.banking.lib.config;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "transaction")
public class TransactionModuleConfig {
    private String defaultCurrencyCode = "AUD";
    private int historyMaxPageSize = 100;
    private int idempotencyTtlHours = 24;

    public String getDefaultCurrencyCode() {
        return defaultCurrencyCode;
    }

    public void setDefaultCurrencyCode(String defaultCurrencyCode) {
        if (defaultCurrencyCode == null || defaultCurrencyCode.isBlank()) {
            this.defaultCurrencyCode = "AUD";
            return;
        }
        this.defaultCurrencyCode = defaultCurrencyCode.trim().toUpperCase(Locale.ROOT);
    }

    public int getHistoryMaxPageSize() {
        return historyMaxPageSize;
    }

    public void setHistoryMaxPageSize(int historyMaxPageSize) {
        this.historyMaxPageSize = historyMaxPageSize;
    }

    public int getIdempotencyTtlHours() {
        return idempotencyTtlHours;
    }

    public void setIdempotencyTtlHours(int idempotencyTtlHours) {
        this.idempotencyTtlHours = idempotencyTtlHours;
    }
}

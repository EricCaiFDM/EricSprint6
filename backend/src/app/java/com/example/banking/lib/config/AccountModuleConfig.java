package com.example.banking.lib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "account")
public class AccountModuleConfig {
    private boolean maskingEnabled = true;

    public boolean isMaskingEnabled() {
        return maskingEnabled;
    }

    public void setMaskingEnabled(boolean maskingEnabled) {
        this.maskingEnabled = maskingEnabled;
    }
}

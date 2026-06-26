package com.example.banking.services;

import org.springframework.stereotype.Service;

import com.example.banking.lib.config.AccountModuleConfig;

@Service
public class AccountFieldMaskingService {
    private final AccountModuleConfig moduleConfig;

    public AccountFieldMaskingService(AccountModuleConfig moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public String maskAccountNumber(String accountNumber) {
        if (!moduleConfig.isMaskingEnabled()) {
            return accountNumber;
        }
        if (accountNumber == null || accountNumber.isBlank()) {
            return null;
        }
        String compact = accountNumber.replaceAll("\\s+", "");
        if (compact.length() <= 4) {
            return "****";
        }
        return "****" + compact.substring(compact.length() - 4);
    }
}

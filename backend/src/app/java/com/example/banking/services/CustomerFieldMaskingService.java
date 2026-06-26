package com.example.banking.services;

import org.springframework.stereotype.Service;

import com.example.banking.lib.config.CustomerModuleConfig;

@Service
public class CustomerFieldMaskingService {
    private final CustomerModuleConfig moduleConfig;

    public CustomerFieldMaskingService(CustomerModuleConfig moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public String maskPhone(String phoneNumber) {
        if (!moduleConfig.isMaskingEnabled()) {
            return phoneNumber;
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        if (phoneNumber.length() < 4) {
            return "****";
        }
        String lastFour = phoneNumber.substring(phoneNumber.length() - 4);
        return "***-***-" + lastFour;
    }

    public String maskEmail(String primaryEmail) {
        if (!moduleConfig.isMaskingEnabled()) {
            return primaryEmail;
        }
        if (primaryEmail == null || primaryEmail.isBlank()) {
            return null;
        }
        int atIndex = primaryEmail.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }
        String prefix = primaryEmail.substring(0, 1);
        String domain = primaryEmail.substring(atIndex);
        return prefix + "***" + domain;
    }
}

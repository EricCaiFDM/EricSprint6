package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.example.banking.lib.config.AccountModuleConfig;

class AccountFieldMaskingServiceTest {

    @Test
    void returnsOriginalWhenMaskingDisabled() {
        AccountFieldMaskingService service = new AccountFieldMaskingService(config(false));

        assertEquals("1234 5678 9012", service.maskAccountNumber("1234 5678 9012"));
        assertNull(service.maskAccountNumber(null));
    }

    @Test
    void returnsNullForNullOrBlankValuesWhenMaskingEnabled() {
        AccountFieldMaskingService service = new AccountFieldMaskingService(config(true));

        assertNull(service.maskAccountNumber(null));
        assertNull(service.maskAccountNumber("   "));
    }

    @Test
    void masksShortAccountNumbersToGenericMask() {
        AccountFieldMaskingService service = new AccountFieldMaskingService(config(true));

        assertEquals("****", service.maskAccountNumber("1234"));
        assertEquals("****", service.maskAccountNumber("12 34"));
    }

    @Test
    void masksLongAccountNumbersWithLastFourDigits() {
        AccountFieldMaskingService service = new AccountFieldMaskingService(config(true));

        assertEquals("****5678", service.maskAccountNumber("12345678"));
        assertEquals("****5678", service.maskAccountNumber("12 34 56 78"));
    }

    private AccountModuleConfig config(boolean maskingEnabled) {
        AccountModuleConfig config = new AccountModuleConfig();
        config.setMaskingEnabled(maskingEnabled);
        return config;
    }
}
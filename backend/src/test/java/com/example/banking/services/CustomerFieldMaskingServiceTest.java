package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.example.banking.lib.config.CustomerModuleConfig;

class CustomerFieldMaskingServiceTest {

    @Test
    void returnsOriginalFieldsWhenMaskingDisabled() {
        CustomerFieldMaskingService service = new CustomerFieldMaskingService(config(false));

        assertEquals("+61 400 123 456", service.maskPhone("+61 400 123 456"));
        assertEquals("customer@example.com", service.maskEmail("customer@example.com"));
    }

    @Test
    void returnsNullForNullOrBlankInputsWhenMaskingEnabled() {
        CustomerFieldMaskingService service = new CustomerFieldMaskingService(config(true));

        assertNull(service.maskPhone(null));
        assertNull(service.maskPhone("   "));
        assertNull(service.maskEmail(null));
        assertNull(service.maskEmail("   "));
    }

    @Test
    void masksPhoneNumbersUsingLastFourDigits() {
        CustomerFieldMaskingService service = new CustomerFieldMaskingService(config(true));

        assertEquals("****", service.maskPhone("123"));
        assertEquals("***-***-3456", service.maskPhone("+61400123456"));
    }

    @Test
    void masksEmailAndHandlesInvalidLocalParts() {
        CustomerFieldMaskingService service = new CustomerFieldMaskingService(config(true));

        assertEquals("c***@example.com", service.maskEmail("customer@example.com"));
        assertEquals("***", service.maskEmail("a@example.com"));
        assertEquals("***", service.maskEmail("@example.com"));
    }

    private CustomerModuleConfig config(boolean maskingEnabled) {
        CustomerModuleConfig config = new CustomerModuleConfig();
        config.setMaskingEnabled(maskingEnabled);
        return config;
    }
}
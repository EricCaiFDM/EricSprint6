package com.example.banking.lib.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.banking.api.common.ApiErrorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class NotificationTemplateSanitizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationTemplateSanitizer sanitizer = new NotificationTemplateSanitizer(objectMapper);

    @Test
    void sanitizeReturnsEmptyJsonWhenContextMissing() {
        assertEquals("{}", sanitizer.sanitize(null));
    }

    @Test
    void sanitizeTrimsKeysAndRetainsValues() throws Exception {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("  customerId  ", "cust-100");
        context.put(null, "anonymous");

        String payload = sanitizer.sanitize(context);
        Map<String, Object> parsed = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
        });

        assertEquals("cust-100", parsed.get("customerId"));
        assertEquals("anonymous", parsed.get(""));
    }

    @Test
    void sanitizeRejectsForbiddenTemplateKeysCaseInsensitive() {
        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> sanitizer.sanitize(Map.of("AuthToken", "abc")));

        assertEquals("NOTIFICATION_VALIDATION_ERROR", exception.getCode());
        assertEquals("templateContext", exception.getField());
        assertTrue(exception.getMessage().contains("AuthToken"));
    }

    @Test
    void sanitizeWrapsSerializationFailureAsValidationError() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("boom") {
                    private static final long serialVersionUID = 1L;
                };
            }
        };
        NotificationTemplateSanitizer failingSanitizer = new NotificationTemplateSanitizer(failingMapper);

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> failingSanitizer.sanitize(Map.of("key", "value")));

        assertEquals("NOTIFICATION_VALIDATION_ERROR", exception.getCode());
        assertEquals("templateContext", exception.getField());
        assertEquals("templateContext must be a valid object", exception.getMessage());
    }
}
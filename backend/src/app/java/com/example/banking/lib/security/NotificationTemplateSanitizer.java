package com.example.banking.lib.security;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.banking.lib.errors.NotificationErrors;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class NotificationTemplateSanitizer {
    private static final Set<String> FORBIDDEN_KEY_PARTS = Set.of(
            "password",
            "secret",
            "token",
            "pin",
            "ssn",
            "cardnumber",
            "cvv");

    private final ObjectMapper objectMapper;

    public NotificationTemplateSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String sanitize(Map<String, Object> context) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (context != null) {
            context.forEach((key, value) -> {
                String safeKey = key == null ? "" : key.trim();
                rejectForbiddenKey(safeKey);
                normalized.put(safeKey, value);
            });
        }

        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw NotificationErrors.validation("templateContext must be a valid object", "templateContext");
        }
    }

    private void rejectForbiddenKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        boolean forbidden = FORBIDDEN_KEY_PARTS.stream().anyMatch(normalized::contains);
        if (forbidden) {
            throw NotificationErrors.validation(
                    "templateContext contains a forbidden key: " + key,
                    "templateContext");
        }
    }
}

package com.example.banking.services;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CustomerPrincipalResolver {
    public CustomerPrincipal resolve(Authentication authentication) {
        if (authentication == null) {
            return new CustomerPrincipal("anonymous", null);
        }

        String userId = "anonymous";
        String role = null;

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            userId = claimAsString(jwt.getClaims(), "sub", authentication.getName());
            role = claimAsString(jwt.getClaims(), "role", null);
        } else {
            userId = authentication.getName() != null ? authentication.getName() : "anonymous";
            role = authorityRole(authentication.getAuthorities());
        }

        if (role == null) {
            role = authorityRole(authentication.getAuthorities());
        }

        if (role != null) {
            role = role.toUpperCase(Locale.ROOT);
        }

        return new CustomerPrincipal(userId, role);
    }

    private String claimAsString(Map<String, Object> claims, String key, String fallback) {
        Object value = claims.get(key);
        if (value == null) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private String authorityRole(Collection<? extends GrantedAuthority> authorities) {
        if (authorities == null) {
            return null;
        }
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.startsWith("ROLE_") ? value.substring(5) : value)
                .findFirst()
                .orElse(null);
    }
}

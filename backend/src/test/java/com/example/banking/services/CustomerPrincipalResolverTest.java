package com.example.banking.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CustomerPrincipalResolverTest {

    private final CustomerPrincipalResolver resolver = new CustomerPrincipalResolver();

    @Test
    void resolvesAnonymousWhenAuthenticationMissing() {
        CustomerPrincipal principal = resolver.resolve(null);

        assertEquals("anonymous", principal.userId());
        assertNull(principal.role());
    }

    @Test
    void resolvesJwtClaimsAndUppercasesRole() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("role", "customer")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of(), "user-1");

        CustomerPrincipal principal = resolver.resolve(authentication);

        assertEquals("user-1", principal.userId());
        assertEquals("CUSTOMER", principal.role());
    }

    @Test
    void fallsBackToAuthenticationNameAndAuthoritiesForNonJwtPrincipals() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("principal", "credentials", "ROLE_admin");
        authentication.setAuthenticated(true);

        CustomerPrincipal principal = resolver.resolve(authentication);

        assertEquals("principal", principal.userId());
        assertEquals("ADMIN", principal.role());
    }

    @Test
    void usesAuthoritiesWhenJwtRoleClaimMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-2")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
            jwt,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
            "user-2");

        CustomerPrincipal principal = resolver.resolve(authentication);

        assertEquals("user-2", principal.userId());
        assertEquals("CUSTOMER", principal.role());
    }

    @Test
    void returnsNullRoleWhenNoAuthoritiesProvided() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("fallback-user", "credentials");
        authentication.setAuthenticated(true);

        CustomerPrincipal principal = resolver.resolve(authentication);

        assertEquals("fallback-user", principal.userId());
        assertNull(principal.role());
    }
}

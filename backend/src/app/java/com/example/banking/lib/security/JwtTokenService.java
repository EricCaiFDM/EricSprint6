package com.example.banking.lib.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {
    private final String issuer;
    private final SecretKey signingKey;
    private final long accessTokenTtlMinutes;
    private final long refreshTokenTtlDays;

    public JwtTokenService(
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
            @Value("${security.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.issuer = issuer;
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public TokenPair issueTokens(String userId, String email, String role) {
        String accessToken = issueToken(userId, email, role, "access", accessTokenTtlMinutes, ChronoUnit.MINUTES);
        String refreshToken = issueToken(userId, email, role, "refresh", refreshTokenTtlDays, ChronoUnit.DAYS);
        return new TokenPair(accessToken, refreshToken);
    }

    public long getAccessTokenExpiresInSeconds() {
        return accessTokenTtlMinutes * 60;
    }

    public RefreshTokenPrincipal validateRefreshToken(String refreshToken) {
        try {
            Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(refreshToken).getPayload();
            String tokenType = claims.get("token_type", String.class);
            if (!Objects.equals("refresh", tokenType)) {
                throw new SecurityException("Invalid or expired refresh token");
            }

            return new RefreshTokenPrincipal(
                    claims.getSubject(),
                    claims.get("email", String.class),
                    claims.get("role", String.class));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new SecurityException("Invalid or expired refresh token");
        }
    }

    private String issueToken(String userId, String email, String role, String tokenType, long ttl, ChronoUnit unit) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl, unit);

        return Jwts.builder()
                .issuer(issuer)
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("email", email)
                .claim("role", role)
                .claim("token_type", tokenType)
                .claim("scope", "ROLE_" + role)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public record TokenPair(String accessToken, String refreshToken) {
    }

    public record RefreshTokenPrincipal(String userId, String email, String role) {
    }
}

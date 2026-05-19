package com.classservice.auth;

import com.classservice.common.exception.JwtExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Utility for signing, parsing, and validating JWTs (JJWT 0.12.x API).
 * JWT claims: sub (userId), tenantId, role.
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expiryHours;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiry-hours:8}") long expiryHours) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryHours = expiryHours;
    }

    public String generateToken(UUID userId, UUID tenantId, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("role", role.name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiryHours, ChronoUnit.HOURS)))
            .signWith(signingKey)
            .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new JwtExpiredException("JWT token has expired");
        } catch (Exception ex) {
            log.debug("JWT parse error: {}", ex.getMessage());
            throw new IllegalArgumentException("Invalid JWT token");
        }
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public UUID extractTenantId(Claims claims) {
        return UUID.fromString(claims.get("tenantId", String.class));
    }

    public UserRole extractRole(Claims claims) {
        return UserRole.valueOf(claims.get("role", String.class));
    }
}

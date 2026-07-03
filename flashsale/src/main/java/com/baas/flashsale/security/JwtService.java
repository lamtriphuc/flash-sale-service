package com.baas.flashsale.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(TenantUserDetails userDetails) {
        return generateToken(userDetails, expirationMs, "access");
    }

    public String generateRefreshToken(TenantUserDetails userDetails) {
        return generateToken(userDetails, refreshExpirationMs, "refresh");
    }

    private String generateToken(TenantUserDetails userDetails, long tokenTtlMs, String tokenType) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + tokenTtlMs);

        return Jwts.builder()
                .subject(userDetails.getEmail())
                .claim("userId", userDetails.getId())
                .claim("tenantId", userDetails.getTenantId())
                .claim("tenantCode", userDetails.getTenantCode())
                .claim("role", userDetails.getRole().name())
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractTenantId(String token) {
        Number tenantId = extractClaim(token, claims -> claims.get("tenantId", Number.class));
        return tenantId == null ? null : tenantId.longValue();
    }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(((TenantUserDetails) userDetails).getEmail())
                && "access".equals(extractTokenType(token))
                && !isTokenExpired(token);
    }

    public boolean isRefreshTokenValid(String token, TenantUserDetails userDetails) {
        return userDetails.getEmail().equals(extractUsername(token))
                && "refresh".equals(extractTokenType(token))
                && !isTokenExpired(token);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}

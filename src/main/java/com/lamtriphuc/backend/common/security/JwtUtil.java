package com.lamtriphuc.backend.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    // Lấy secret key từ application.yml (Cần chuỗi > 32 ký tự)
    @Value("${jwt.secret:defaultSecretKeyVeryLongAndSecureString123456789}")
    private String jwtSecret;

    @Value("${jwt.expirationMs:86400000}") // Mặc định 1 ngày
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateToken(UUID accountId, UUID tenantId, String email, String role) {
        return Jwts.builder()
                .subject(accountId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateAndExtractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }
}
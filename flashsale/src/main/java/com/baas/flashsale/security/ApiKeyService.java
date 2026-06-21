package com.baas.flashsale.security;

import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.tenant.entity.ApiKey;
import com.baas.flashsale.tenant.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyContext authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new BusinessException("INVALID_API_KEY", HttpStatus.UNAUTHORIZED, "Missing X-API-Key");
        }

        ApiKey apiKey = apiKeyRepository.findByKeyValueAndActiveTrue(hashApiKey(rawApiKey))
                .orElseThrow(() -> new BusinessException("INVALID_API_KEY", HttpStatus.UNAUTHORIZED, "Invalid API key"));

        if (apiKey.getExpiredAt() != null && apiKey.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("INVALID_API_KEY", HttpStatus.UNAUTHORIZED, "API key expired");
        }

        if (!Boolean.TRUE.equals(apiKey.getTenant().getActive())) {
            throw new BusinessException("TENANT_SUSPENDED", HttpStatus.FORBIDDEN, "Tenant is suspended");
        }

        return new ApiKeyContext(apiKey);
    }

    private String hashApiKey(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

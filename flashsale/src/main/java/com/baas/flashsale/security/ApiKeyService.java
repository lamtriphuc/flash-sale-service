package com.baas.flashsale.security;

import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.tenant.entity.ApiKey;
import com.baas.flashsale.tenant.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyHasher apiKeyHasher;

    public ApiKeyContext authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new BusinessException("INVALID_API_KEY", HttpStatus.UNAUTHORIZED, "Missing X-API-Key");
        }

        ApiKey apiKey = apiKeyRepository.findByKeyValueAndActiveTrue(apiKeyHasher.hash(rawApiKey))
                .orElseThrow(() -> new BusinessException("INVALID_API_KEY", HttpStatus.UNAUTHORIZED, "Invalid API key"));

        if (apiKey.getExpiredAt() != null && apiKey.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("INVALID_API_KEY", HttpStatus.UNAUTHORIZED, "API key expired");
        }

        if (!Boolean.TRUE.equals(apiKey.getTenant().getActive())) {
            throw new BusinessException("TENANT_SUSPENDED", HttpStatus.FORBIDDEN, "Tenant is suspended");
        }

        return new ApiKeyContext(apiKey);
    }
}

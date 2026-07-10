package com.lamtriphuc.backend.tenant.repository;

import com.lamtriphuc.backend.tenant.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByApiKeyHashAndIsActiveTrue(String apiKeyHash);
}

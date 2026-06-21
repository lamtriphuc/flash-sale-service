package com.baas.flashsale.tenant.repository;

import com.baas.flashsale.tenant.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyValueAndActiveTrue(String keyValue);

    boolean existsByKeyValue(String keyValue);

    List<ApiKey> findByTenantId(Long tenantId);
}

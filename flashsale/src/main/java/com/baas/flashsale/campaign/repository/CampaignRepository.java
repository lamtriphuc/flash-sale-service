package com.baas.flashsale.campaign.repository;

import com.baas.flashsale.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    boolean existsByTenantIdAndCode(Long tenantId, String code);

    Optional<Campaign> findByIdAndTenantId(Long id, Long tenantId);

    List<Campaign> findByTenantId(Long tenantId);
}

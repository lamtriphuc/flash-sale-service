package com.lamtriphuc.backend.campaign.repository;

import com.lamtriphuc.backend.campaign.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
    List<Campaign> findByTenantId(UUID tenantId);
    Optional<Campaign> findByIdAndTenantId(UUID id, UUID tenantId);
}

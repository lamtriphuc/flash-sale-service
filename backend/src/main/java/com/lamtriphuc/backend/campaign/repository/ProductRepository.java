package com.lamtriphuc.backend.campaign.repository;

import com.lamtriphuc.backend.campaign.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByCampaignIdAndTenantId(UUID campaignId, UUID tenantId);
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
}

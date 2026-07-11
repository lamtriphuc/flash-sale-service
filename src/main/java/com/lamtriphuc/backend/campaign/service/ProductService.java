package com.lamtriphuc.backend.campaign.service;

import com.lamtriphuc.backend.campaign.dto.ProductAddRequest;
import com.lamtriphuc.backend.campaign.dto.ProductResponse;
import com.lamtriphuc.backend.campaign.entity.Campaign;
import com.lamtriphuc.backend.campaign.entity.Product;
import com.lamtriphuc.backend.campaign.repository.CampaignRepository;
import com.lamtriphuc.backend.campaign.repository.ProductRepository;
import com.lamtriphuc.backend.common.exception.AppException;
import com.lamtriphuc.backend.common.exception.ErrorCode;
import com.lamtriphuc.backend.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CampaignRepository campaignRepository;

    @Transactional
    public ProductResponse addProductToCampaign(UUID campaignId, ProductAddRequest request) {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();

        Campaign campaign = campaignRepository.findByIdAndTenantId(campaignId, currentTenantId)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));

        Product product = Product.builder()
                .tenantId(currentTenantId)
                .campaign(campaign)
                .sku(request.getSku())
                .name(request.getName())
                .price(request.getPrice())
                .totalStock(request.getQuantity())
                .availableStock(request.getQuantity())
                .build();

        product = productRepository.save(product);

        // [QUAN TRỌNG] Ở ĐÂY SẼ CÓ LOGIC ĐẨY TỒN KHO VÀO REDIS (Sẽ triển khai ở bài toán xử lý Tồn kho)

        return ProductResponse.fromEntity(product);
    }

    public List<ProductResponse> getProductsByCampaign(UUID campaignId) {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();

        List<Product> products = productRepository
                .findByCampaignIdAndTenantId(campaignId, currentTenantId);

        return products.stream().map(ProductResponse::fromEntity).toList();
    }
}

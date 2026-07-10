package com.lamtriphuc.backend.campaign.service;

import com.lamtriphuc.backend.campaign.dto.CampaignResponse;
import com.lamtriphuc.backend.campaign.dto.ProductResponse;
import com.lamtriphuc.backend.campaign.dto.PublicCampaignDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicCampaignService {
    private final CampaignService campaignService;
    private final ProductService productService;

    public PublicCampaignDetailResponse getCampaignDetailsForLandingPage(UUID campaignId) {
        CampaignResponse campaign = campaignService.getCampaignById(campaignId);
        List<ProductResponse> products = productService.getProductsByCampaign(campaignId);

        return PublicCampaignDetailResponse.builder()
                .campaign(campaign)
                .products(products)
                .build();
    }
}

package com.lamtriphuc.backend.campaign.service;

import com.lamtriphuc.backend.campaign.dto.CampaignCreateRequest;
import com.lamtriphuc.backend.campaign.dto.CampaignResponse;
import com.lamtriphuc.backend.campaign.entity.Campaign;
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
public class CampaignService {
    private final CampaignRepository campaignRepository;
    private final ProductRepository productRepository;

    @Transactional
    public CampaignResponse createCampaign(CampaignCreateRequest request) {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }

        Campaign campaign = Campaign.builder()
                .tenantId(currentTenantId)
                .name(request.getName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status("PENDING")
                .build();

        campaign = campaignRepository.save(campaign);

        return CampaignResponse.fromEntity(campaign);
    }

    public List<CampaignResponse> getAllCampaigns() {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();

        List<Campaign> campaigns = campaignRepository.findByTenantId(currentTenantId);

        return campaigns.stream()
                .map(CampaignResponse::fromEntity)
                .toList();
    }

    public CampaignResponse getCampaignById(UUID campaignId) {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();

        Campaign campaign = campaignRepository.findByIdAndTenantId(campaignId, currentTenantId)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));

        return CampaignResponse.fromEntity(campaign);
    }
}

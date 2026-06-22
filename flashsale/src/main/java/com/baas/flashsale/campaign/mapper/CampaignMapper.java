package com.baas.flashsale.campaign.mapper;

import com.baas.flashsale.campaign.dto.CampaignResponse;
import com.baas.flashsale.campaign.entity.Campaign;
import com.baas.flashsale.campaign.entity.CampaignStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CampaignMapper {
    public CampaignResponse toResponse(Campaign campaign) {
        return CampaignResponse.builder()
                .id(campaign.getId())
                .tenantId(campaign.getTenant().getId())
                .code(campaign.getCode())
                .name(campaign.getName())
                .status(resolveStatus(campaign))
                .startTime(campaign.getStartTime())
                .endTime(campaign.getEndTime())
                .createdAt(campaign.getCreatedAt())
                .build();
    }

    public CampaignStatus resolveStatus(Campaign campaign) {
        if (campaign.getStatus() == CampaignStatus.CANCELLED) {
            return CampaignStatus.CANCELLED;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(campaign.getStartTime())) {
            return CampaignStatus.DRAFT;
        }
        if (now.isAfter(campaign.getEndTime())) {
            return CampaignStatus.ENDED;
        }
        return CampaignStatus.ACTIVE;
    }
}

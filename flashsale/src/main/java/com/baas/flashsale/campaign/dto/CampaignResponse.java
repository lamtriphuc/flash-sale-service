package com.baas.flashsale.campaign.dto;

import com.baas.flashsale.campaign.entity.CampaignStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CampaignResponse {
    private Long id;
    private Long tenantId;
    private String code;
    private String name;
    private CampaignStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
}

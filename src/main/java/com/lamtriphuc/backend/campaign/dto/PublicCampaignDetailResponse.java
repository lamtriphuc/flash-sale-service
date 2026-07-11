package com.lamtriphuc.backend.campaign.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PublicCampaignDetailResponse {
    private CampaignResponse campaign;
    private List<ProductResponse> products;
}

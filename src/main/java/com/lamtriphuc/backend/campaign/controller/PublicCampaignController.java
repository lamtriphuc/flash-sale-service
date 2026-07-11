package com.lamtriphuc.backend.campaign.controller;

import com.lamtriphuc.backend.campaign.dto.PublicCampaignDetailResponse;
import com.lamtriphuc.backend.campaign.service.PublicCampaignService;
import com.lamtriphuc.backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/campaigns")
@RequiredArgsConstructor
public class PublicCampaignController {
    private final PublicCampaignService publicCampaignService;

    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<PublicCampaignDetailResponse>> getCampaignDetails(
            @PathVariable UUID campaignId) {

        PublicCampaignDetailResponse response = publicCampaignService.getCampaignDetailsForLandingPage(campaignId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

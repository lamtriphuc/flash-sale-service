package com.lamtriphuc.backend.campaign.controller;

import com.lamtriphuc.backend.campaign.dto.CampaignCreateRequest;
import com.lamtriphuc.backend.campaign.dto.ProductAddRequest;
import com.lamtriphuc.backend.campaign.dto.CampaignResponse;
import com.lamtriphuc.backend.campaign.dto.ProductResponse;
import com.lamtriphuc.backend.campaign.service.CampaignService;
import com.lamtriphuc.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request
    ) {
        CampaignResponse response = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> getAllCampaigns() {
        List<CampaignResponse> responses = campaignService.getAllCampaigns();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaignById(
            @PathVariable UUID campaignId) {
        CampaignResponse response = campaignService.getCampaignById(campaignId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
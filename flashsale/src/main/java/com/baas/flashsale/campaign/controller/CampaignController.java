package com.baas.flashsale.campaign.controller;

import com.baas.flashsale.campaign.service.CampaignService;
import com.baas.flashsale.campaign.dto.CampaignResponse;
import com.baas.flashsale.campaign.dto.CreateCampaignRequest;
import com.baas.flashsale.flashsale.dto.CreateFlashSaleItemRequest;
import com.baas.flashsale.flashsale.dto.FlashSaleItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.createCampaign(request));
    }

    @GetMapping("/{campaignId}")
    public CampaignResponse getCampaign(
            @PathVariable Long campaignId
    ) {
        return campaignService.getCampaign(campaignId);
    }

    @PostMapping("/{campaignId}/items")
    public ResponseEntity<FlashSaleItemResponse> addItem(
            @PathVariable Long campaignId,
            @Valid @RequestBody CreateFlashSaleItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.addItem(campaignId, request));
    }

    @GetMapping("/{campaignId}/items")
    public List<FlashSaleItemResponse> getItems(
            @PathVariable Long campaignId
    ) {
        return campaignService.getItems(campaignId);
    }
}

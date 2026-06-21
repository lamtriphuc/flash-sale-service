package com.baas.flashsale.campaign;

import com.baas.flashsale.campaign.dto.CampaignResponse;
import com.baas.flashsale.campaign.dto.CreateCampaignRequest;
import com.baas.flashsale.flashsale.dto.CreateFlashSaleItemRequest;
import com.baas.flashsale.flashsale.dto.FlashSaleItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;

    @PostMapping
    public CampaignResponse createCampaign(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        return campaignService.createCampaign(apiKey, request);
    }

    @GetMapping("/{campaignId}")
    public CampaignResponse getCampaign(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long campaignId
    ) {
        return campaignService.getCampaign(apiKey, campaignId);
    }

    @PostMapping("/{campaignId}/items")
    public FlashSaleItemResponse addItem(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long campaignId,
            @Valid @RequestBody CreateFlashSaleItemRequest request
    ) {
        return campaignService.addItem(apiKey, campaignId, request);
    }

    @GetMapping("/{campaignId}/items")
    public List<FlashSaleItemResponse> getItems(
            @RequestHeader("X-API-Key") String apiKey,
            @PathVariable Long campaignId
    ) {
        return campaignService.getItems(apiKey, campaignId);
    }
}

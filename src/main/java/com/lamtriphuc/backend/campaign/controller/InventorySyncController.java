package com.lamtriphuc.backend.campaign.controller;

import com.lamtriphuc.backend.campaign.service.InventoryCacheService;
import com.lamtriphuc.backend.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class InventorySyncController {
    private final InventoryCacheService inventoryCacheService;

    // POST /api/v1/campaigns/{campaignId}/sync-inventory
    @PostMapping("/{campaignId}/sync-inventory")
    public ResponseEntity<ApiResponse<String>> syncInventory(@PathVariable UUID campaignId) {

        int syncedItems = inventoryCacheService.syncCampaignInventoryToRedis(campaignId);

        String message = String.format("Đã đồng bộ thành công %d sản phẩm lên máy chủ bộ nhớ đệm (Redis).", syncedItems);

        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
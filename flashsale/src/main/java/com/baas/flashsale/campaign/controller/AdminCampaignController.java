package com.baas.flashsale.campaign.controller;

import com.baas.flashsale.campaign.dto.ThumbnailUploadResponse;
import com.baas.flashsale.campaign.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/campaigns")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCampaignController {
    private final CampaignService campaignService;

    @PostMapping("/{campaignId}/thumbnail")
    public ThumbnailUploadResponse uploadThumbnail(
            @PathVariable Long campaignId,
            @RequestParam("file") MultipartFile file
    ) {
        return campaignService.uploadThumbnail(campaignId, file);
    }
}

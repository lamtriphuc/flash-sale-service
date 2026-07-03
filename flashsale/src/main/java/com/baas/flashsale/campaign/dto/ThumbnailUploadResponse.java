package com.baas.flashsale.campaign.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ThumbnailUploadResponse {
    private Long campaignId;
    private String thumbnailUrl;
    private String thumbnailPublicId;
}

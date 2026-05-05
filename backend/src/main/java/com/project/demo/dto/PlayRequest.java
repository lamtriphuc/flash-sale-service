package com.project.demo.dto;

import lombok.Data;

@Data
public class PlayRequest {
    private Long campaignId;
    private Long prizeId;
    private String idempotencyKey;
}

package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayMessage {
    private Long userId;
    private Long campaignId;
    private Long prizeId;
    private String idempotencyKey;
}

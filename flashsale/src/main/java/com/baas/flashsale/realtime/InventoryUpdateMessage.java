package com.baas.flashsale.realtime;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InventoryUpdateMessage {
    private Long campaignId;
    private Long itemId;
    private String itemCode;
    private Integer remainingQuantity;
    private LocalDateTime updatedAt;
}

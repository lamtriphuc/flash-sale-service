package com.baas.flashsale.flashsale.dto;

import com.baas.flashsale.flashsale.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderResponse {
    private Long id;
    private Long tenantId;
    private Long campaignId;
    private Long itemId;
    private String itemCode;
    private String userId;
    private OrderStatus status;
    private String failReason;
    private LocalDateTime createdAt;
}

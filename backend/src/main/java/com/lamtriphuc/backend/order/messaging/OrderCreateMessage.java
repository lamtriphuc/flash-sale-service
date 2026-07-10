package com.lamtriphuc.backend.order.messaging;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class OrderCreateMessage {
    private UUID tenantId;
    private UUID campaignId;
    private UUID productId;
    private UUID userId;
    private BigDecimal unitPrice;
    private Integer quantity;
    private String idempotencyKey;
}
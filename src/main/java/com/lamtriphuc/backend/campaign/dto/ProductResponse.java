package com.lamtriphuc.backend.campaign.dto;

import com.lamtriphuc.backend.campaign.entity.Product;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ProductResponse {
    private UUID id;
    private UUID campaignId;
    private String sku;
    private String name;
    private BigDecimal price;
    private Integer totalStock;
    private Integer availableStock;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .campaignId(product.getCampaign().getId())
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .totalStock(product.getTotalStock())
                .availableStock(product.getAvailableStock())
                .build();
    }
}
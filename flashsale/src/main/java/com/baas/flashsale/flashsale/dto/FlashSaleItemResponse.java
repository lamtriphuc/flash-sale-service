package com.baas.flashsale.flashsale.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FlashSaleItemResponse {
    private Long id;
    private Long campaignId;
    private String itemCode;
    private String itemName;
    private Long originalPrice;
    private Long salePrice;
    private Integer totalQuantity;
    private Integer remainingQuantity;
    private Boolean active;
}

package com.baas.flashsale.flashsale.mapper;

import com.baas.flashsale.flashsale.dto.FlashSaleItemResponse;
import com.baas.flashsale.flashsale.entity.FlashSaleItem;
import org.springframework.stereotype.Component;

@Component
public class FlashSaleItemMapper {
    public FlashSaleItemResponse toResponse(FlashSaleItem item) {
        return FlashSaleItemResponse.builder()
                .id(item.getId())
                .campaignId(item.getCampaign().getId())
                .itemCode(item.getItemCode())
                .itemName(item.getItemName())
                .originalPrice(item.getOriginalPrice())
                .salePrice(item.getSalePrice())
                .totalQuantity(item.getTotalQuantity())
                .remainingQuantity(item.getRemainingQuantity())
                .active(item.getActive())
                .build();
    }
}

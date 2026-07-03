package com.baas.flashsale.flashsale.mapper;

import com.baas.flashsale.flashsale.dto.OrderResponse;
import com.baas.flashsale.flashsale.entity.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {
    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .tenantId(order.getTenant().getId())
                .campaignId(order.getCampaign().getId())
                .itemId(order.getItem().getId())
                .itemCode(order.getItem().getItemCode())
                .userId(order.getParticipant().getExternalCustomerId())
                .status(order.getStatus())
                .failReason(order.getFailReason())
                .paymentExpiresAt(order.getPaymentExpiresAt())
                .confirmedAt(order.getConfirmedAt())
                .cancelledAt(order.getCancelledAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}

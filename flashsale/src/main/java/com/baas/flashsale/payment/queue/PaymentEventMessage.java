package com.baas.flashsale.payment.queue;

public record PaymentEventMessage(
        Long orderId,
        Long campaignId,
        Long itemId,
        String userId,
        PaymentEventType type
) {
}

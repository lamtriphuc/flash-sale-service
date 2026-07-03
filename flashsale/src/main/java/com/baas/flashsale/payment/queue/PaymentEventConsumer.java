package com.baas.flashsale.payment.queue;

import com.baas.flashsale.flashsale.entity.Order;
import com.baas.flashsale.flashsale.repository.FlashSaleItemRepository;
import com.baas.flashsale.flashsale.repository.OrderRepository;
import com.baas.flashsale.flashsale.service.InventoryGateService;
import com.baas.flashsale.payment.service.PaymentNotificationService;
import com.baas.flashsale.realtime.InventoryRealtimePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {
    private final OrderRepository orderRepository;
    private final FlashSaleItemRepository itemRepository;
    private final InventoryGateService inventoryGateService;
    private final InventoryRealtimePublisher inventoryRealtimePublisher;
    private final PaymentNotificationService notificationService;

    @RabbitListener(queues = PaymentQueueConfig.QUEUE)
    @Transactional
    public void handle(PaymentEventMessage message) {
        Order order = orderRepository.findById(message.orderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + message.orderId()));

        if (message.type() == PaymentEventType.PAYMENT_SUCCESS) {
            log.info("Payment success for order {}", order.getId());
            notificationService.sendPaymentSuccess(order);
            return;
        }

        boolean released = inventoryGateService.releaseReservation(message.campaignId(), message.itemId(), message.userId());
        if (released) {
            itemRepository.incrementRemainingQuantity(message.itemId());
            order.getItem().setRemainingQuantity(order.getItem().getRemainingQuantity() + 1);
            inventoryRealtimePublisher.publishAfterCommit(order.getItem());
        }
        notificationService.sendPaymentFailed(order);
        log.info("Payment cancelled for order {} by {}", order.getId(), message.type());
    }
}

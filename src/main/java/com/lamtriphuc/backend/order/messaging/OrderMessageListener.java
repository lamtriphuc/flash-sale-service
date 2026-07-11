package com.lamtriphuc.backend.order.messaging;

import com.lamtriphuc.backend.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageListener {
    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATE_QUEUE)
    public void receiveOrderCreateMessage(OrderCreateMessage message) {
        log.info("Worker nhận được yêu cầu tạo đơn hàng cho User: {} - Product: {}",
                message.getUserId(), message.getProductId());

        try {
            orderService.createOrder(message);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý đơn hàng từ Queue. IdempotencyKey: {}", message.getIdempotencyKey(), e);
        }
    }
}

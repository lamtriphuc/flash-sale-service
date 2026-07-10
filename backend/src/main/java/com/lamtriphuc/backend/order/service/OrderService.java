package com.lamtriphuc.backend.order.service;

import com.lamtriphuc.backend.common.exception.AppException;
import com.lamtriphuc.backend.common.exception.ErrorCode;
import com.lamtriphuc.backend.notification.service.NotificationService;
import com.lamtriphuc.backend.order.entity.Order;
import com.lamtriphuc.backend.order.messaging.OrderCreateMessage;
import com.lamtriphuc.backend.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Transactional
    public void createOrder(OrderCreateMessage message) {
        BigDecimal totalAmount = message.getUnitPrice().multiply(BigDecimal.valueOf(message.getQuantity()));

        Order order = Order.builder()
                .tenantId(message.getTenantId())
                .campaignId(message.getCampaignId())
                .productId(message.getProductId())
                .userId(message.getUserId())
                .unitPrice(message.getUnitPrice())
                .quantity(message.getQuantity())
                .totalAmount(totalAmount)
                .idempotencyKey(message.getIdempotencyKey())
                .status("RESERVED")
                .queueMessageId(null)
                .build();

        try {
            orderRepository.save(order);
            log.info("Lưu đơn hàng thành công vào DB. User: {}", message.getUserId());

            notificationService.notifyOrderSuccess(
                    message.getUserId(),
                    message.getIdempotencyKey(),
                    order.getId()
            );
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage() != null && e.getMessage().contains("uk_orders_idempotency")) {
                log.warn("Trùng lặp IdempotencyKey bị bỏ qua: {}", message.getIdempotencyKey());
            } else {
                throw new AppException(ErrorCode.DATABASE_ERROR);
            }
        }
    }
}

package com.lamtriphuc.backend.order.service;

import com.lamtriphuc.backend.common.exception.AppException;
import com.lamtriphuc.backend.common.exception.ErrorCode;
import com.lamtriphuc.backend.notification.service.NotificationService;
import com.lamtriphuc.backend.order.entity.Order;
import com.lamtriphuc.backend.order.entity.OrderStatus;
import com.lamtriphuc.backend.order.messaging.OrderCreateMessage;
import com.lamtriphuc.backend.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

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
                .status(OrderStatus.RESERVED)
                .queueMessageId(null)
                .build();

        try {
            orderRepository.save(order);
            log.info("Lưu đơn hàng thành công vào DB. User: {}", message.getUserId());

            String timeoutAlarmKey = String.format("order_timeout:%s", order.getId());
            redisTemplate.opsForValue().set(timeoutAlarmKey, "1", 300, TimeUnit.SECONDS);

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

    @Transactional
    public void cancelExpiredOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        if (!OrderStatus.RESERVED.equals(order.getStatus())) {
            return;
        }

        try {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            // Định nghĩa lại Keys trong Redis
            String stockKey = String.format("stock:%s:%s", order.getTenantId(), order.getProductId());
            String userBoughtKey = String.format("bought:%s:%s:%s",
                    order.getTenantId(), order.getProductId(), order.getUserId());

            // Hoàn lại kho trong Redis (Cộng thêm)
            redisTemplate.opsForValue().increment(stockKey, order.getQuantity());

            // Mở khóa cho End-user (Xóa key giữ chỗ để họ có thể bấm mua lại)
            redisTemplate.delete(userBoughtKey);

            log.info("Đã Hủy đơn {} và hoàn {} sản phẩm vào Redis.", order.getId(), order.getQuantity());
        } catch (Exception e) {
            log.error("Lỗi khi hoàn kho cho đơn hàng {}: {}", order.getId(), e.getMessage());
        }
    }

    @Transactional
    public void confirmPaymentSuccess(UUID orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND);
        }

        Order order = orderOpt.get();

        if (OrderStatus.RESERVED.equals(order.getStatus())) {

            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            // Tắt báo thức hủy đơn trên Redis
            String timeoutAlarmKey = String.format("order_timeout:%s", orderId);
            redisTemplate.delete(timeoutAlarmKey);

            log.info("Mock Payment: Chốt đơn thành công! Đã nhận tiền cho đơn: {}", orderId);
        } else {
            log.warn("Mock Payment: Đơn hàng {} không thể thanh toán (Trạng thái hiện tại: {})",
                    orderId, order.getStatus());
        }
    }
}

package com.lamtriphuc.backend.order.messaging;

import com.lamtriphuc.backend.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisKeyExpirationListener implements MessageListener {
    private final OrderService orderService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // Lấy tên Key vừa bị hết hạn (Ví dụ: order_timeout:123e4567-e89b-12d3-a456-426614174000)
        String expiredKey = new String(message.getBody(), StandardCharsets.UTF_8);

        // Chỉ xử lý các key có tiền tố là order_timeout
        if (expiredKey.startsWith("order_timeout:")) {
            try {
                String orderIdStr = expiredKey.replace("order_timeout:", "");
                UUID orderId = UUID.fromString(orderIdStr);

                log.info("Redis báo hiệu Đơn hàng {} đã quá hạn thanh toán. Tiến hành hủy và hoàn kho.", orderId);

                // Gọi sang Service để thực hiện Hủy đơn (Cập nhật DB và cộng lại Redis)
                orderService.cancelExpiredOrder(orderId);

            } catch (Exception e) {
                log.error("Lỗi khi xử lý sự kiện Redis Expired cho key {}: {}", expiredKey, e.getMessage());
            }
        }
    }
}

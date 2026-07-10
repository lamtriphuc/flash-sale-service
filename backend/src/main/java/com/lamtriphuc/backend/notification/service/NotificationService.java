package com.lamtriphuc.backend.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;

    // 1. Broadcast (Gửi cho tất cả): Báo cập nhật tồn kho hoặc HẾT HÀNG
    public void broadcastStockUpdate(UUID campaignId, UUID productId, int currentStock) {
        String topic = String.format("/topic/campaigns/%s/stock", campaignId);
        String message = String.format("{\"productId\":\"%s\", \"stock\":%d}", productId, currentStock);

        messagingTemplate.convertAndSend(topic, message);
    }

    // 2. Private (Gửi cá nhân): Báo cho End-User biết đơn hàng đã được ghi vào DB thành công
    public void notifyOrderSuccess(UUID userId, String idempotencyKey, UUID orderId) {
        // Spring Boot sẽ tự động map đường dẫn này tới đúng Session của userId đó
        String destination = "/queue/orders";
        String message = String.format(
                "{\"status\":\"SUCCESS\", \"idempotencyKey\":\"%s\", \"orderId\":\"%s\"}",
                idempotencyKey, orderId
        );

        // Gửi đích danh tới userId (Tương ứng với accountId trong token)
        messagingTemplate.convertAndSendToUser(userId.toString(), destination, message);
    }
}

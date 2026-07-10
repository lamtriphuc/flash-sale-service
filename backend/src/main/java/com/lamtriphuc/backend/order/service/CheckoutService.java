package com.lamtriphuc.backend.order.service;

import com.lamtriphuc.backend.campaign.entity.Product;
import com.lamtriphuc.backend.campaign.repository.ProductRepository;
import com.lamtriphuc.backend.common.exception.AppException;
import com.lamtriphuc.backend.common.exception.ErrorCode;
import com.lamtriphuc.backend.common.security.SecurityUtils;
import com.lamtriphuc.backend.order.dto.CheckoutRequest;
import com.lamtriphuc.backend.order.messaging.OrderCreateMessage;
import com.lamtriphuc.backend.order.messaging.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ProductRepository productRepository;

    public void processCheckout(CheckoutRequest request) {
        UUID currentTenantId = SecurityUtils.getCurrentTenantId();

        // CustomUserDetails chứa accountId (tương đương với userId đối với End-user)
        UUID currentUserId = SecurityUtils.getCurrentAccountId();

        // 1. Lấy thông tin sản phẩm (Thực tế nên lấy từ Redis Cache, ở đây query DB để đơn giản hóa)
        Product product = productRepository.findByIdAndTenantId(request.getProductId(), currentTenantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. Chuẩn bị Keys cho Lua Script
        String stockKey = String.format("stock:%s:%s", currentTenantId, product.getId());
        String userBoughtKey = String.format("bought:%s:%s:%s", currentTenantId, product.getId(), currentUserId);

        // 3. Load và chạy Lua Script
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/checkout.lua")));
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                List.of(stockKey, userBoughtKey), // Truyền KEYS
                String.valueOf(request.getQuantity()) // Truyền ARGV
        );

        // 4. Xử lý kết quả từ Redis
        if (result == null) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (result == 0L) {
            throw new AppException(ErrorCode.OUT_OF_STOCK); // Mã 409: Hết hàng
        } else if (result == -1L) {
            throw new AppException(ErrorCode.ALREADY_BOUGHT); // Mã 409: Bạn đã mua rồi
        }

        // 5. Nếu result == 1 (Thành công) -> Đẩy tin nhắn vào RabbitMQ
        OrderCreateMessage message = OrderCreateMessage.builder()
                .tenantId(currentTenantId)
                .campaignId(product.getCampaign().getId())
                .productId(product.getId())
                .userId(currentUserId)
                .unitPrice(product.getPrice())
                .quantity(request.getQuantity())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_CREATE_QUEUE, message);

        log.info("Đã đẩy đơn hàng giữ chỗ vào Queue. User: {}, Product: {}", currentUserId, product.getId());
    }
}

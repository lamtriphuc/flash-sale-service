package com.baas.flashsale.flashsale.service;

import com.baas.flashsale.common.BusinessException;
import com.baas.flashsale.redis.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryGateService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> reserveInventoryScript;
    private final RedisKeyBuilder redisKeyBuilder;

    public void initializeStock(Long campaignId, Long itemId, int quantity) {
        try {
            redisTemplate.opsForValue().setIfAbsent(redisKeyBuilder.stockKey(campaignId, itemId), String.valueOf(quantity));
        } catch (DataAccessException ex) {
            throw inventoryGateUnavailable(ex);
        }
    }

    public InventoryGateResult reserve(Long campaignId, Long itemId, String userId, int initialStock) {
        List<?> result;
        try {
            result = redisTemplate.execute(
                    reserveInventoryScript,
                    List.of(redisKeyBuilder.stockKey(campaignId, itemId), redisKeyBuilder.buyersKey(campaignId)),
                    userId,
                    String.valueOf(initialStock)
            );
        } catch (DataAccessException ex) {
            throw inventoryGateUnavailable(ex);
        }

        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Redis inventory gate returned an invalid result");
        }

        InventoryGateResult.Status status = InventoryGateResult.Status.valueOf(String.valueOf(result.get(0)));
        int remainingQuantity = Integer.parseInt(String.valueOf(result.get(1)));
        return new InventoryGateResult(status, remainingQuantity);
    }

    public void releaseReservation(Long campaignId, Long itemId, String userId) {
        try {
            redisTemplate.opsForValue().increment(redisKeyBuilder.stockKey(campaignId, itemId));
            redisTemplate.opsForSet().remove(redisKeyBuilder.buyersKey(campaignId), userId);
        } catch (DataAccessException ex) {
            throw inventoryGateUnavailable(ex);
        }
    }

    private BusinessException inventoryGateUnavailable(DataAccessException ex) {
        return new BusinessException(
                "INVENTORY_GATE_UNAVAILABLE",
                HttpStatus.SERVICE_UNAVAILABLE,
                "Redis inventory gate is unavailable"
        );
    }
}

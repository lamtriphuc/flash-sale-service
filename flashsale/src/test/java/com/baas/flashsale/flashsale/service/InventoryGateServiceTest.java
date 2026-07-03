package com.baas.flashsale.flashsale.service;

import com.baas.flashsale.redis.RedisKeyBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryGateServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private DefaultRedisScript<List> reserveInventoryScript;

    @Mock
    private DefaultRedisScript<List> releaseInventoryScript;

    @Mock
    private RedisKeyBuilder redisKeyBuilder;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private InventoryGateService inventoryGateService;

    @Test
    void reserveReturnsReservedWhenRedisScriptReservesStock() {
        mockKeys();
        when(redisTemplate.execute(eq(reserveInventoryScript), any(List.class), eq("user-1"), eq("100")))
                .thenReturn(List.of("RESERVED", "99"));

        InventoryGateResult result = inventoryGateService.reserve(1L, 10L, "user-1", 100);

        assertThat(result.status()).isEqualTo(InventoryGateResult.Status.RESERVED);
        assertThat(result.remainingQuantity()).isEqualTo(99);
    }

    @Test
    void reserveReturnsOutOfStockWhenRedisScriptRejectsByStock() {
        mockKeys();
        when(redisTemplate.execute(eq(reserveInventoryScript), any(List.class), eq("user-1"), eq("0")))
                .thenReturn(List.of("OUT_OF_STOCK", "0"));

        InventoryGateResult result = inventoryGateService.reserve(1L, 10L, "user-1", 0);

        assertThat(result.status()).isEqualTo(InventoryGateResult.Status.OUT_OF_STOCK);
        assertThat(result.remainingQuantity()).isZero();
    }

    @Test
    void reserveReturnsAlreadyPurchasedWhenRedisScriptRejectsDuplicateBuyer() {
        mockKeys();
        when(redisTemplate.execute(eq(reserveInventoryScript), any(List.class), eq("user-1"), eq("100")))
                .thenReturn(List.of("ALREADY_PURCHASED", "99"));

        InventoryGateResult result = inventoryGateService.reserve(1L, 10L, "user-1", 100);

        assertThat(result.status()).isEqualTo(InventoryGateResult.Status.ALREADY_PURCHASED);
        assertThat(result.remainingQuantity()).isEqualTo(99);
    }

    @Test
    void initializeStockSetsStockOnlyWhenMissing() {
        when(redisKeyBuilder.stockKey(1L, 10L)).thenReturn("stock-key");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        inventoryGateService.initializeStock(1L, 10L, 100);

        verify(valueOperations).setIfAbsent("stock-key", "100");
    }

    @Test
    void releaseReservationIncrementsStockAndRemovesBuyer() {
        mockKeys();
        when(redisTemplate.execute(eq(releaseInventoryScript), any(List.class), eq("user-1")))
                .thenReturn(List.of("RELEASED", "100"));

        inventoryGateService.releaseReservation(1L, 10L, "user-1");

        verify(redisTemplate).execute(eq(releaseInventoryScript), any(List.class), eq("user-1"));
    }

    private void mockKeys() {
        when(redisKeyBuilder.stockKey(1L, 10L)).thenReturn("stock-key");
        when(redisKeyBuilder.buyersKey(1L)).thenReturn("buyers-key");
    }
}

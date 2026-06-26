package com.baas.flashsale.redis;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {
    public String stockKey(Long campaignId, Long itemId) {
        return "flashsale:campaign:%d:item:%d:stock".formatted(campaignId, itemId);
    }

    public String buyersKey(Long campaignId) {
        return "flashsale:campaign:%d:buyers".formatted(campaignId);
    }
}

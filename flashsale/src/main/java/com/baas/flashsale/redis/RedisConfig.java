package com.baas.flashsale.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

@Configuration
public class RedisConfig {
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public DefaultRedisScript<List> reserveInventoryScript() {
        return new DefaultRedisScript<>("""
                local stockKey = KEYS[1]
                local buyersKey = KEYS[2]
                local userId = ARGV[1]
                local initialStock = tonumber(ARGV[2])

                if redis.call('EXISTS', stockKey) == 0 then
                  redis.call('SET', stockKey, initialStock)
                end

                if redis.call('SISMEMBER', buyersKey, userId) == 1 then
                  return {'ALREADY_PURCHASED', redis.call('GET', stockKey)}
                end

                local stock = tonumber(redis.call('GET', stockKey))
                if stock <= 0 then
                  return {'OUT_OF_STOCK', stock}
                end

                local remaining = redis.call('DECR', stockKey)
                redis.call('SADD', buyersKey, userId)
                return {'RESERVED', remaining}
                """, List.class);
    }

    @Bean
    public DefaultRedisScript<List> releaseInventoryScript() {
        return new DefaultRedisScript<>("""
                local stockKey = KEYS[1]
                local buyersKey = KEYS[2]
                local userId = ARGV[1]

                if redis.call('SISMEMBER', buyersKey, userId) == 0 then
                  return {'NOT_RESERVED', redis.call('GET', stockKey) or '0'}
                end

                local remaining = redis.call('INCR', stockKey)
                redis.call('SREM', buyersKey, userId)
                return {'RELEASED', remaining}
                """, List.class);
    }
}

package com.lamtriphuc.backend.common.config;

import com.lamtriphuc.backend.order.messaging.RedisKeyExpirationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisMessageListenerContainer keyExpirationListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisKeyExpirationListener redisKeyExpirationListener
    ) {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);

        // Lắng nghe sự kiện expired
        listenerContainer.addMessageListener(
                redisKeyExpirationListener,
                new PatternTopic("__keyevent@0__:expired")
        );
        return listenerContainer;
    }
}

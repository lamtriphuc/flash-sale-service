package com.lamtriphuc.backend.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Frontend sẽ kết nối tới ws://domain.com/ws-endpoint
        registry.addEndpoint("/ws-endpoint")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic: Dành cho broadcast (ví dụ: /topic/campaigns/123/stock) - Ai cũng nghe được
        // /queue: Dành cho cá nhân (ví dụ: gửi riêng cho user A)
        registry.enableSimpleBroker("/topic", "/queue");

        // Tiền tố khi Client gửi tin nhắn lên Server (Nếu cần)
        registry.setApplicationDestinationPrefixes("/app");

        // Cấu hình tiền tố cho kênh cá nhân
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Đăng ký Lưới lọc Token vào luồng nhận tin
        registration.interceptors(webSocketAuthInterceptor);
    }
}

package com.example.chat_server.config;

import com.example.chat_server.security.JwtChannelInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Hỗ trợ /topic cho nhóm/public và /queue cho chat riêng 1-1
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user"); // Tiền tố cho tin nhắn cá nhân
    }

    // Frontend (Vercel) kết nối WebSocket thẳng tới server nên phải liệt kê origin được phép
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }

    // Đăng ký JwtChannelInterceptor để kiểm tra token trước khi cho phép kết nối WebSocket
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }
}
package com.example.booking.config;

import com.example.booking.ws.RoomWsHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RoomWsHandler roomWsHandler;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            RoomWsHandler roomWsHandler,
            @Value("${ws.allowed-origin-patterns:*}") String allowedOriginPatterns) {
        this.roomWsHandler = roomWsHandler;
        this.allowedOrigins = allowedOriginPatterns.split(",");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(roomWsHandler, "/ws/rooms")
                .setAllowedOriginPatterns(allowedOrigins);
    }
}
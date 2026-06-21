package com.auctionsystem.auction.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registra el endpoint WebSocket nativo para las pujas en tiempo real.
 * Endpoint: ws://host:8080/ws/subastas?subastaId={id}&token={jwt}
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final BidWebSocketHandler bidWebSocketHandler;
    private final AuctionHandshakeInterceptor auctionHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(bidWebSocketHandler, "/ws/subastas")
                .addInterceptors(auctionHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}

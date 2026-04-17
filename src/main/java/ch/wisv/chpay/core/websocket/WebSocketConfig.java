package ch.wisv.chpay.core.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LedWebSocketHandler ledHandler;

    public WebSocketConfig(LedWebSocketHandler ledHandler) {
        this.ledHandler = ledHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(ledHandler, "/ws/ledstrip")
                .setAllowedOrigins("chpay.ch.tudelft.nl")  // Adjust for security (e.g., specific ESP IPs)
                .withSockJS();  // Optional: Enables SockJS fallback for older browsers
    }
}

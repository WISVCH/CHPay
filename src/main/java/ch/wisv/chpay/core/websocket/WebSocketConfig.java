package ch.wisv.chpay.core.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final LedWebSocketHandler ledHandler;
  private final String applicationBaseUrl;

  public WebSocketConfig(
      LedWebSocketHandler ledHandler,
      @Value("${spring.application.baseurl}") String applicationBaseUrl) {
    this.ledHandler = ledHandler;
    this.applicationBaseUrl = applicationBaseUrl;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry
        .addHandler(ledHandler, "/api/v1/leds/stream")
        .setAllowedOriginPatterns(applicationBaseUrl)
        .withSockJS(); // Optional: Enables SockJS fallback for older browsers
  }
}

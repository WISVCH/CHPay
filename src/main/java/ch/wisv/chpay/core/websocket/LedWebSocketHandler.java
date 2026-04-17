package ch.wisv.chpay.core.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class LedWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(LedWebSocketHandler.class);
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private static final Map<WebSocketSession, Instant> lastActivity = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int TIMEOUT_SECONDS = 68;  // Close after 68s of inactivity
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LedWebSocketHandler() {
        // Schedule a task to check for timeouts every 30 seconds
        scheduler.scheduleAtFixedRate(this::checkTimeouts, 30, 30, TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        lastActivity.put(session, Instant.now());
        logger.info("WebSocket connection established. Total connections: {}", sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        lastActivity.remove(session);
        logger.info("WebSocket connection closed. Total connections: {}", sessions.size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        lastActivity.put(session, Instant.now());  // Update activity on any message

        if (payload != null && "ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
            logger.debug("Responded to ping with pong for session: {}", session.getId());
        } else {
            logger.warn("Unknown message received: {}", payload);
        }
    }

    private void checkTimeouts() {
        Instant now = Instant.now();
        sessions.forEach(session -> {
            Instant last = lastActivity.get(session);
            if (last != null && now.isAfter(last.plusSeconds(TIMEOUT_SECONDS))) {
                try {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    logger.info("Closed inactive WebSocket session: {}", session.getId());
                } catch (IOException e) {
                    logger.error("Error closing timed-out session", e);
                }
            }
        });
    }

    // Method to broadcast payment success message to all active sessions
    public void broadcastPaymentSuccess(int r, int g, int b, String pattern) {
        Map<String, Object> message = Map.of(
            "r", r,
            "g", g,
            "b", b,
            "pattern", pattern
        );
        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            sessions.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    logger.error("Error sending message to session: {}", session.getId(), e);
                }
            });
            logger.info("Broadcasted payment success to {} connections", sessions.size());
        } catch (Exception e) {
            logger.error("Error serializing broadcast message", e);
        }
    }

    // Getter for connection count (optional, for monitoring)
    public int getConnectionCount() {
        return sessions.size();
    }
}

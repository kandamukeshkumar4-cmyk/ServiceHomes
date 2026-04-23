package com.servicehomes.api.notifications.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketEndpoint extends Endpoint {

    public static final String PATH = "/ws/notifications";

    private final NotificationWebSocketSessionRegistry sessionRegistry;
    private final NotificationWebSocketPrincipalResolver principalResolver;
    private final ObjectMapper objectMapper;

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        Optional<UUID> resolvedUserId;
        try {
            resolvedUserId = principalResolver.resolve(session);
        } catch (RuntimeException ex) {
            log.warn("Failed to resolve notification WebSocket principal: {}", ex.getMessage());
            close(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication required");
            return;
        }

        if (resolvedUserId.isEmpty()) {
            close(session, CloseReason.CloseCodes.VIOLATED_POLICY, "Authentication required");
            return;
        }

        UUID userId = resolvedUserId.get();
        sessionRegistry.register(userId, session);
        session.addMessageHandler(String.class, message -> handleClientMessage(session, message));
        sendConnectionAck(session, userId);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        sessionRegistry.unregister(session);
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        if (throwable != null) {
            log.warn("Notification WebSocket error for session {}: {}", sessionId(session), throwable.getMessage());
        }
        if (session != null) {
            sessionRegistry.unregister(session);
        }
    }

    private void handleClientMessage(Session session, String message) {
        if (message == null || !"ping".equalsIgnoreCase(message.trim())) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "pong");
        payload.put("sentAt", Instant.now().toString());
        sendJson(session, payload);
    }

    private void sendConnectionAck(Session session, UUID userId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "connection_ack");
        payload.put("userId", userId.toString());
        payload.put("connectedAt", Instant.now().toString());
        sendJson(session, payload);
    }

    private void sendJson(Session session, Object payload) {
        try {
            session.getAsyncRemote().sendText(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize notification WebSocket message: {}", ex.getMessage());
        }
    }

    private void close(Session session, CloseReason.CloseCode closeCode, String reason) {
        try {
            session.close(new CloseReason(closeCode, reason));
        } catch (IOException ex) {
            log.warn("Failed to close unauthorized notification WebSocket session: {}", ex.getMessage());
        }
    }

    private String sessionId(Session session) {
        return session != null ? session.getId() : "unknown";
    }
}

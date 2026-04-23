package com.servicehomes.api.notifications.infrastructure;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
class NotificationWebSocketSessionRegistry {

    private final ConcurrentMap<UUID, Set<Session>> sessionsByUserId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> userIdBySessionId = new ConcurrentHashMap<>();

    void register(UUID userId, Session session) {
        sessionsByUserId.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        userIdBySessionId.put(session.getId(), userId);
    }

    void unregister(Session session) {
        UUID userId = userIdBySessionId.remove(session.getId());
        if (userId != null) {
            unregister(userId, session);
        }
    }

    boolean send(UUID userId, String payload) {
        Set<Session> sessions = sessionsByUserId.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }

        boolean sent = false;
        for (Session session : sessions) {
            if (!session.isOpen()) {
                unregister(userId, session);
                continue;
            }

            session.getAsyncRemote().sendText(payload, result -> {
                if (!result.isOK()) {
                    log.warn("Failed to deliver notification WebSocket message to session {}: {}",
                        session.getId(), result.getException() != null ? result.getException().getMessage() : "unknown");
                    unregister(userId, session);
                }
            });
            sent = true;
        }
        return sent;
    }

    private void unregister(UUID userId, Session session) {
        Set<Session> sessions = sessionsByUserId.get(userId);
        if (sessions == null) {
            return;
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUserId.remove(userId, sessions);
        }
    }
}

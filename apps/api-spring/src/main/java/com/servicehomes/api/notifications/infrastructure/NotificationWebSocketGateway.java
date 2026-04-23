package com.servicehomes.api.notifications.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.notifications.application.NotificationDispatcher;
import com.servicehomes.api.notifications.application.dto.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
class NotificationWebSocketGateway implements NotificationDispatcher.RealtimeNotificationGateway {

    private final NotificationWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public boolean send(NotificationMessage message) {
        try {
            return sessionRegistry.send(message.recipientUserId(), objectMapper.writeValueAsString(payload(message)));
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize notification {} for WebSocket delivery: {}", message.id(), ex.getMessage());
            return false;
        }
    }

    private Map<String, Object> payload(NotificationMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.id());
        payload.put("type", message.type().name());
        payload.put("title", message.title());
        payload.put("body", message.body());
        payload.put("data", data(message));
        payload.put("channel", "IN_APP");
        payload.put("createdAt", message.createdAt());
        payload.put("read", false);
        return payload;
    }

    private Map<String, Object> data(NotificationMessage message) {
        Map<String, Object> data = new LinkedHashMap<>(message.metadata());
        putIfPresent(data, "actionUrl", message.actionUrl());
        putIfPresent(data, "resourceType", message.resourceType());
        putIfPresent(data, "resourceId", message.resourceId());
        putIfPresent(data, "senderUserId", message.senderUserId());
        putIfPresent(data, "threadId", message.threadId());
        putIfPresent(data, "messageId", message.messageId());
        putIfPresent(data, "reservationId", message.reservationId());
        putIfPresent(data, "listingId", message.listingId());
        putIfPresent(data, "dedupeKey", message.dedupeKey());
        return data;
    }

    private void putIfPresent(Map<String, Object> data, String key, String value) {
        if (value != null && !value.isBlank()) {
            data.put(key, value);
        }
    }

    private void putIfPresent(Map<String, Object> data, String key, UUID value) {
        if (value != null) {
            data.put(key, value.toString());
        }
    }
}

package com.servicehomes.api.notifications.application.dto;

import com.servicehomes.api.notifications.domain.NotificationType;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record NotificationMessage(
    UUID id,
    UUID recipientUserId,
    String recipientEmail,
    String recipientName,
    UUID senderUserId,
    NotificationType type,
    String title,
    String body,
    String actionUrl,
    String resourceType,
    UUID resourceId,
    UUID threadId,
    UUID messageId,
    UUID reservationId,
    UUID listingId,
    String dedupeKey,
    Map<String, Object> metadata,
    Instant createdAt
) {
    public NotificationMessage {
        if (recipientUserId == null) {
            throw new IllegalArgumentException("recipientUserId is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }

        id = id != null ? id : UUID.randomUUID();
        title = requiredText(title, "title");
        body = requiredText(body, "body");
        recipientEmail = trimToNull(recipientEmail);
        recipientName = trimToNull(recipientName);
        actionUrl = trimToNull(actionUrl);
        resourceType = trimToNull(resourceType);
        if (listingId == null && resourceId != null && "listing".equalsIgnoreCase(resourceType)) {
            listingId = resourceId;
        }
        if (reservationId == null && resourceId != null && "reservation".equalsIgnoreCase(resourceType)) {
            reservationId = resourceId;
        }
        dedupeKey = trimToNull(dedupeKey);
        metadata = immutableData(metadata);
        createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public NotificationMessage withId(UUID nextId) {
        return new NotificationMessage(
            nextId,
            recipientUserId,
            recipientEmail,
            recipientName,
            senderUserId,
            type,
            title,
            body,
            actionUrl,
            resourceType,
            resourceId,
            threadId,
            messageId,
            reservationId,
            listingId,
            dedupeKey,
            metadata,
            createdAt
        );
    }

    private static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Map<String, Object> immutableData(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(copy);
    }
}

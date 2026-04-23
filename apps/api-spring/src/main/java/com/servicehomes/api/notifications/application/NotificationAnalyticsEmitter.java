package com.servicehomes.api.notifications.application;

import com.servicehomes.api.analytics.application.EventPublisher;
import com.servicehomes.api.notifications.application.dto.NotificationMessage;
import com.servicehomes.api.notifications.domain.Notification;
import com.servicehomes.api.notifications.domain.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationAnalyticsEmitter {

    private static final String NOTIFICATION_AGGREGATE_TYPE = "notification";

    private final EventPublisher eventPublisher;

    public void deliveryRequested(NotificationMessage notification, NotificationChannel channel) {
        eventPublisher.publish(
            "notification_delivery_requested",
            NOTIFICATION_AGGREGATE_TYPE,
            notification.id().toString(),
            extendedDeliveryPayload(notification, channel)
        );
    }

    public void deliverySucceeded(
        NotificationMessage notification,
        NotificationChannel channel,
        String provider,
        String providerMessageId
    ) {
        Map<String, Object> payload = extendedDeliveryPayload(notification, channel);
        putIfPresent(payload, "provider", provider);
        putIfPresent(payload, "providerMessageId", providerMessageId);
        payload.put("attemptNumber", 1);

        eventPublisher.publish(
            "notification_delivery_succeeded",
            NOTIFICATION_AGGREGATE_TYPE,
            notification.id().toString(),
            payload
        );
    }

    public void deliveryFailed(
        NotificationMessage notification,
        NotificationChannel channel,
        String provider,
        String failureCode,
        String failureReason,
        boolean retryable
    ) {
        Map<String, Object> payload = extendedDeliveryPayload(notification, channel);
        putIfPresent(payload, "provider", provider);
        putIfPresent(payload, "failureCode", failureCode);
        putIfPresent(payload, "failureReason", failureReason);
        payload.put("retryable", retryable);
        payload.put("attemptNumber", 1);

        eventPublisher.publish(
            "notification_delivery_failed",
            NOTIFICATION_AGGREGATE_TYPE,
            notification.id().toString(),
            payload
        );
    }

    public void notificationRead(Notification notification) {
        engagement(notification, "READ");
    }

    public void notificationDismissed(Notification notification) {
        engagement(notification, "DISMISSED");
    }

    public void notificationsReadAll(UUID userId, int changedCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("timestamp", Instant.now().toString());
        payload.put("notificationId", userId.toString());
        payload.put("notificationType", "SYSTEM");
        payload.put("engagementType", "READ_ALL");
        payload.put("channel", NotificationChannel.IN_APP.name());
        payload.put("recipientUserId", userId.toString());
        payload.put("changedCount", changedCount);

        eventPublisher.publish(
            "notification_engagement_recorded",
            NOTIFICATION_AGGREGATE_TYPE,
            userId.toString(),
            payload
        );
    }

    private void engagement(Notification notification, String engagementType) {
        Map<String, Object> payload = deliveryPayload(
            notification.getId(),
            notification.getType().name(),
            notification.getChannel(),
            notification.getUserId()
        );
        payload.put("engagementType", engagementType);

        eventPublisher.publish(
            "notification_engagement_recorded",
            NOTIFICATION_AGGREGATE_TYPE,
            notification.getId().toString(),
            payload
        );
    }

    private Map<String, Object> deliveryPayload(NotificationMessage notification, NotificationChannel channel) {
        return deliveryPayload(
            notification.id(),
            notification.type().name(),
            channel,
            notification.recipientUserId()
        );
    }

    private Map<String, Object> deliveryPayload(
        UUID notificationId,
        String notificationType,
        NotificationChannel channel,
        UUID recipientUserId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", UUID.randomUUID().toString());
        payload.put("timestamp", Instant.now().toString());
        payload.put("notificationId", notificationId.toString());
        payload.put("notificationType", notificationType);
        payload.put("channel", channel.name());
        payload.put("recipientUserId", recipientUserId.toString());
        return payload;
    }

    private Map<String, Object> extendedDeliveryPayload(NotificationMessage notification, NotificationChannel channel) {
        Map<String, Object> payload = deliveryPayload(notification, channel);
        putIfPresent(payload, "senderUserId", notification.senderUserId());
        putIfPresent(payload, "threadId", notification.threadId());
        putIfPresent(payload, "messageId", notification.messageId());
        putIfPresent(payload, "reservationId", notification.reservationId());
        putIfPresent(payload, "listingId", notification.listingId());
        putIfPresent(payload, "dedupeKey", notification.dedupeKey());
        return payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }

    private void putIfPresent(Map<String, Object> payload, String key, UUID value) {
        if (value != null) {
            payload.put(key, value.toString());
        }
    }
}

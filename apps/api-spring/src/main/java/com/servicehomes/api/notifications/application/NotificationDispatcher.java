package com.servicehomes.api.notifications.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.notifications.application.dto.NotificationMessage;
import com.servicehomes.api.notifications.domain.Notification;
import com.servicehomes.api.notifications.domain.NotificationChannel;
import com.servicehomes.api.notifications.domain.NotificationPolicy;
import com.servicehomes.api.notifications.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final NotificationPolicy notificationPolicy;
    private final ObjectMapper objectMapper;
    private final RealtimeNotificationGateway realtimeNotificationGateway;
    private final EmailNotificationService emailNotificationService;
    private final NotificationAnalyticsEmitter analyticsEmitter;

    @Transactional
    public DispatchResult dispatch(NotificationMessage message) {
        NotificationMessage notification = Objects.requireNonNull(message, "message is required");

        ChannelDelivery inAppDelivery = deliverInApp(notification);
        ChannelDelivery emailDelivery = deliverEmail(notification);

        UUID notificationId = inAppDelivery.notificationId() != null
            ? inAppDelivery.notificationId()
            : emailDelivery.notificationId();
        if (notificationId == null) {
            notificationId = notification.id();
        }

        return new DispatchResult(
            notificationId,
            inAppDelivery.delivered(),
            inAppDelivery.realtimeDelivered(),
            emailDelivery.delivered(),
            emailDelivery.emailDelivery
        );
    }

    private ChannelDelivery deliverInApp(NotificationMessage notification) {
        if (!shouldDeliver(notification, NotificationChannel.IN_APP)) {
            return ChannelDelivery.skipped(EmailNotificationService.EmailDeliveryResult.skipped("preference_or_dedupe"));
        }

        Notification saved = notificationRepository.save(toEntity(notification, NotificationChannel.IN_APP));
        NotificationMessage persistedNotification = notification.withId(saved.getId());
        analyticsEmitter.deliveryRequested(persistedNotification, NotificationChannel.IN_APP);
        boolean realtimeDelivered = realtimeNotificationGateway.send(persistedNotification);
        analyticsEmitter.deliverySucceeded(
            persistedNotification,
            NotificationChannel.IN_APP,
            realtimeDelivered ? "websocket" : "notification_center",
            null
        );

        return new ChannelDelivery(saved.getId(), true, realtimeDelivered,
            EmailNotificationService.EmailDeliveryResult.skipped("not_email_channel"));
    }

    private ChannelDelivery deliverEmail(NotificationMessage notification) {
        if (!emailNotificationService.isEnabled()) {
            return ChannelDelivery.skipped(EmailNotificationService.EmailDeliveryResult.skipped("email_disabled"));
        }

        if (!shouldDeliver(notification, NotificationChannel.EMAIL)) {
            return ChannelDelivery.skipped(EmailNotificationService.EmailDeliveryResult.skipped("preference_or_dedupe"));
        }

        Notification saved = notificationRepository.save(toEntity(notification, NotificationChannel.EMAIL));
        NotificationMessage persistedNotification = notification.withId(saved.getId());
        EmailNotificationService.EmailDeliveryResult emailDelivery = emailNotificationService.send(persistedNotification);
        if (!emailDelivery.attempted()) {
            return new ChannelDelivery(saved.getId(), true, false, emailDelivery);
        }

        analyticsEmitter.deliveryRequested(persistedNotification, NotificationChannel.EMAIL);
        if (emailDelivery.sent()) {
            analyticsEmitter.deliverySucceeded(
                persistedNotification,
                NotificationChannel.EMAIL,
                "ses",
                emailDelivery.providerMessageId()
            );
        } else {
            analyticsEmitter.deliveryFailed(
                persistedNotification,
                NotificationChannel.EMAIL,
                "ses",
                emailDelivery.reason(),
                emailDelivery.reason(),
                emailDelivery.retryable()
            );
        }

        return new ChannelDelivery(saved.getId(), emailDelivery.sent(), false, emailDelivery);
    }

    private boolean shouldDeliver(NotificationMessage notification, NotificationChannel channel) {
        return notificationPolicy.enabled(notification.recipientUserId(), notification.type(), channel)
            && !notificationPolicy.recentlyDelivered(notification.recipientUserId(), notification.type(), channel);
    }

    private Notification toEntity(NotificationMessage notification, NotificationChannel channel) {
        return Notification.builder()
            .userId(notification.recipientUserId())
            .type(notification.type())
            .title(notification.title())
            .body(notification.body())
            .dataJson(toJson(notificationData(notification)))
            .channel(channel)
            .build();
    }

    private Map<String, Object> notificationData(NotificationMessage notification) {
        Map<String, Object> data = new LinkedHashMap<>(notification.metadata());
        putIfPresent(data, "actionUrl", notification.actionUrl());
        putIfPresent(data, "resourceType", notification.resourceType());
        putIfPresent(data, "resourceId", notification.resourceId());
        putIfPresent(data, "senderUserId", notification.senderUserId());
        putIfPresent(data, "threadId", notification.threadId());
        putIfPresent(data, "messageId", notification.messageId());
        putIfPresent(data, "reservationId", notification.reservationId());
        putIfPresent(data, "listingId", notification.listingId());
        putIfPresent(data, "dedupeKey", notification.dedupeKey());
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

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : Map.of());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Notification data must be JSON serializable", ex);
        }
    }

    public interface RealtimeNotificationGateway {
        boolean send(NotificationMessage message);
    }

    public record DispatchResult(
        UUID notificationId,
        boolean inAppStored,
        boolean realtimeDelivered,
        boolean emailDelivered,
        EmailNotificationService.EmailDeliveryResult emailDelivery
    ) {}

    private record ChannelDelivery(
        UUID notificationId,
        boolean delivered,
        boolean realtimeDelivered,
        EmailNotificationService.EmailDeliveryResult emailDelivery
    ) {
        static ChannelDelivery skipped(EmailNotificationService.EmailDeliveryResult result) {
            return new ChannelDelivery(null, false, false, result);
        }
    }
}

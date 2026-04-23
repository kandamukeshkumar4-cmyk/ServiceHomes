package com.servicehomes.api.notifications.application.dto;

import com.servicehomes.api.notifications.domain.NotificationChannel;
import com.servicehomes.api.notifications.domain.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    NotificationType type,
    String title,
    String body,
    Map<String, Object> data,
    NotificationChannel channel,
    Instant createdAt,
    Instant readAt,
    boolean read
) {}

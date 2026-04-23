package com.servicehomes.api.notifications.application.dto;

import com.servicehomes.api.notifications.domain.NotificationChannel;
import com.servicehomes.api.notifications.domain.NotificationType;

public record NotificationPreferenceDto(
    NotificationType type,
    NotificationChannel channel,
    boolean enabled
) {}

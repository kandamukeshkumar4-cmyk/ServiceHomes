package com.servicehomes.api.notifications.application.dto;

import com.servicehomes.api.notifications.domain.NotificationChannel;
import com.servicehomes.api.notifications.domain.NotificationType;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
    @NotNull NotificationType type,
    @NotNull NotificationChannel channel,
    boolean enabled
) {}

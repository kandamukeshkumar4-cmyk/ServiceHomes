package com.servicehomes.api.notifications.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPolicy {

    public static final int MAX_BATCH_SIZE = 50;
    private static final Duration DEDUP_WINDOW = Duration.ofMinutes(5);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public boolean enabled(UUID userId, NotificationType type, NotificationChannel channel) {
        NotificationPreferenceId id = new NotificationPreferenceId(userId, channel, type);
        return preferenceRepository.findById(id)
            .map(NotificationPreference::isEnabled)
            .orElse(true);
    }

    public boolean recentlyDelivered(UUID userId, NotificationType type, NotificationChannel channel) {
        return notificationRepository.existsByUserIdAndTypeAndChannelAndCreatedAtAfter(
            userId,
            type,
            channel,
            Instant.now().minus(DEDUP_WINDOW)
        );
    }

    public int boundedPageSize(int requestedSize) {
        return Math.min(Math.max(requestedSize, 1), MAX_BATCH_SIZE);
    }
}

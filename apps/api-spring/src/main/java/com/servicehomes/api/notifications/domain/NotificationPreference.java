package com.servicehomes.api.notifications.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @EmbeddedId
    private NotificationPreferenceId id;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static NotificationPreference enabled(UUID userId, NotificationType type, NotificationChannel channel) {
        return NotificationPreference.builder()
            .id(new NotificationPreferenceId(userId, channel, type))
            .enabled(true)
            .updatedAt(Instant.now())
            .build();
    }
}

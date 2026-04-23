package com.servicehomes.api.notifications.application;

import com.servicehomes.api.notifications.application.dto.NotificationPreferenceDto;
import com.servicehomes.api.notifications.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationPreferencesService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public List<NotificationPreferenceDto> getPreferences(UUID userId) {
        Map<NotificationPreferenceId, NotificationPreference> existing = new HashMap<>();
        for (NotificationPreference preference : preferenceRepository.findByIdUserId(userId)) {
            existing.put(preference.getId(), preference);
        }

        List<NotificationPreferenceDto> preferences = new ArrayList<>();
        for (NotificationType type : NotificationType.values()) {
            for (NotificationChannel channel : NotificationChannel.values()) {
                NotificationPreferenceId id = new NotificationPreferenceId(userId, channel, type);
                boolean enabled = existing.getOrDefault(id, NotificationPreference.enabled(userId, type, channel)).isEnabled();
                preferences.add(new NotificationPreferenceDto(type, channel, enabled));
            }
        }
        return preferences;
    }

    @Transactional
    public NotificationPreferenceDto updatePreference(
        UUID userId,
        NotificationType type,
        NotificationChannel channel,
        boolean enabled
    ) {
        NotificationPreferenceId id = new NotificationPreferenceId(userId, channel, type);
        NotificationPreference preference = preferenceRepository.findById(id)
            .orElseGet(() -> NotificationPreference.enabled(userId, type, channel));
        preference.setEnabled(enabled);
        preference.setUpdatedAt(Instant.now());
        preferenceRepository.save(preference);
        return new NotificationPreferenceDto(type, channel, enabled);
    }
}

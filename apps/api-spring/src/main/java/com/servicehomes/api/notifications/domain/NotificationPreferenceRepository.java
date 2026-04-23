package com.servicehomes.api.notifications.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, NotificationPreferenceId> {

    List<NotificationPreference> findByIdUserId(UUID userId);
}

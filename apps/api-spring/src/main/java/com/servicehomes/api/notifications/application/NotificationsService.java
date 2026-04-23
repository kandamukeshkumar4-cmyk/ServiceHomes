package com.servicehomes.api.notifications.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicehomes.api.notifications.application.dto.NotificationDto;
import com.servicehomes.api.notifications.domain.Notification;
import com.servicehomes.api.notifications.domain.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationsService {

    private final NotificationRepository notificationRepository;
    private final NotificationAnalyticsEmitter analyticsEmitter;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<NotificationDto> listUnread(UUID userId, Pageable pageable) {
        return notificationRepository
            .findByUserIdAndReadAtIsNullAndDismissedAtIsNullOrderByCreatedAtDesc(userId, pageable)
            .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> listAll(UUID userId, Pageable pageable) {
        return notificationRepository
            .findByUserIdAndDismissedAtIsNullOrderByCreatedAtDesc(userId, pageable)
            .map(this::toDto);
    }

    @Transactional
    public NotificationDto markAsRead(UUID userId, UUID notificationId) {
        Notification notification = findForUser(userId, notificationId);
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            analyticsEmitter.notificationRead(notification);
        }
        return toDto(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {
        int changed = notificationRepository.markAllUnreadAsRead(userId, Instant.now());
        analyticsEmitter.notificationsReadAll(userId, changed);
        return changed;
    }

    @Transactional
    public void dismiss(UUID userId, UUID notificationId) {
        Notification notification = findForUser(userId, notificationId);
        notification.setDismissedAt(Instant.now());
        if (notification.getReadAt() == null) {
            notification.setReadAt(notification.getDismissedAt());
        }
        analyticsEmitter.notificationDismissed(notification);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNullAndDismissedAtIsNull(userId);
    }

    private Notification findForUser(UUID userId, UUID notificationId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
            .filter(notification -> notification.getDismissedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getBody(),
            data(notification),
            notification.getChannel(),
            notification.getCreatedAt(),
            notification.getReadAt(),
            notification.isRead()
        );
    }

    @SneakyThrows
    private Map<String, Object> data(Notification notification) {
        return objectMapper.readValue(notification.getDataJson(), new TypeReference<>() {});
    }
}

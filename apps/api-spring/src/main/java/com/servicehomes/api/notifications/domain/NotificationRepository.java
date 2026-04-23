package com.servicehomes.api.notifications.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdAndDismissedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Notification> findByUserIdAndReadAtIsNullAndDismissedAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndReadAtIsNullAndDismissedAtIsNull(UUID userId);

    boolean existsByUserIdAndTypeAndChannelAndCreatedAtAfter(
        UUID userId,
        NotificationType type,
        NotificationChannel channel,
        Instant createdAt
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.readAt = :readAt
        WHERE n.userId = :userId
          AND n.readAt IS NULL
          AND n.dismissedAt IS NULL
        """)
    int markAllUnreadAsRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}

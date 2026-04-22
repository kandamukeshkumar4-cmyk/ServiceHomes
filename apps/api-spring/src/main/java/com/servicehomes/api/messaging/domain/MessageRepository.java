package com.servicehomes.api.messaging.domain;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByThreadIdOrderByCreatedAtAsc(UUID threadId);

    Optional<Message> findTopByThreadIdOrderByCreatedAtDesc(UUID threadId);

    long countByThreadIdAndSenderIdNotAndReadAtIsNull(UUID threadId, UUID senderId);

    @Query("""
        SELECT MAX(m.readAt)
        FROM Message m
        WHERE m.thread.id = :threadId
          AND m.senderId <> :recipientId
          AND m.readAt IS NOT NULL
        """)
    Instant findLatestReadAtForRecipient(@Param("threadId") UUID threadId, @Param("recipientId") UUID recipientId);

    @Query("""
        SELECT COUNT(m)
        FROM Message m
        WHERE m.thread.id = :threadId
          AND m.senderId <> :recipientId
          AND m.readAt IS NULL
          AND m.createdAt >= :cutoff
          AND m.id <> :excludedMessageId
        """)
    long countRecentUnreadMessages(
        @Param("threadId") UUID threadId,
        @Param("recipientId") UUID recipientId,
        @Param("cutoff") Instant cutoff,
        @Param("excludedMessageId") UUID excludedMessageId
    );

    @Modifying
    @Transactional
    @Query("""
        UPDATE Message m
        SET m.readAt = :readAt
        WHERE m.thread.id = :threadId
          AND m.senderId <> :userId
          AND m.readAt IS NULL
        """)
    int markIncomingMessagesRead(
        @Param("threadId") UUID threadId,
        @Param("userId") UUID userId,
        @Param("readAt") Instant readAt
    );
}

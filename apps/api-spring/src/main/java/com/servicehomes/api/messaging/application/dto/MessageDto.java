package com.servicehomes.api.messaging.application.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
    UUID id,
    UUID senderId,
    String senderDisplayName,
    String senderAvatarUrl,
    String content,
    Instant createdAt,
    Instant readAt,
    boolean mine
) {}

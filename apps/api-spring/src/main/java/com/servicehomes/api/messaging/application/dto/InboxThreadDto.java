package com.servicehomes.api.messaging.application.dto;

import java.time.Instant;
import java.util.UUID;

public record InboxThreadDto(
    UUID threadId,
    UUID reservationId,
    UUID listingId,
    String listingTitle,
    String listingCoverUrl,
    UUID counterpartId,
    String counterpartName,
    String counterpartAvatarUrl,
    String lastMessagePreview,
    Instant lastMessageAt,
    long unreadCount
) {}

package com.servicehomes.api.messaging.application.dto;

import java.util.List;
import java.util.UUID;

public record MessageThreadDto(
    UUID threadId,
    UUID reservationId,
    UUID listingId,
    String listingTitle,
    String listingCoverUrl,
    UUID guestId,
    UUID hostId,
    UUID counterpartId,
    String counterpartName,
    String counterpartAvatarUrl,
    long unreadCount,
    List<MessageDto> messages
) {}

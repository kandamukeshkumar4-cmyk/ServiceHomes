package com.servicehomes.api.reviews.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewDto(
    UUID id,
    UUID reservationId,
    UUID listingId,
    UUID guestId,
    int rating,
    String comment,
    String guestDisplayName,
    String guestAvatarUrl,
    String hostResponse,
    Instant createdAt,
    Instant updatedAt
) {}

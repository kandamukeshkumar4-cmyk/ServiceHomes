package com.servicehomes.api.reviews.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewDto(
    UUID id,
    UUID reservationId,
    UUID listingId,
    UUID guestId,
    UUID hostId,
    UUID reviewerId,
    String reviewerRole,
    int rating,
    Integer cleanlinessRating,
    Integer accuracyRating,
    Integer communicationRating,
    Integer locationRating,
    Integer valueRating,
    String comment,
    String guestDisplayName,
    String guestAvatarUrl,
    String hostResponse,
    Instant visibleAt,
    String moderationStatus,
    int reportCount,
    Instant createdAt,
    Instant updatedAt
) {}

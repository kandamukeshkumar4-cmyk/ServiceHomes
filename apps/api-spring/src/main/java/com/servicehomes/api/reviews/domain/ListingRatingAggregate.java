package com.servicehomes.api.reviews.domain;

import java.util.UUID;

public record ListingRatingAggregate(
    UUID listingId,
    Double averageRating,
    long reviewCount,
    Double cleanlinessRating,
    Double accuracyRating,
    Double communicationRating,
    Double locationRating,
    Double valueRating
) {}

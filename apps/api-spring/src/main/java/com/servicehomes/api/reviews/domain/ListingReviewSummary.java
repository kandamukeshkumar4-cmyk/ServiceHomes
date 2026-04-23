package com.servicehomes.api.reviews.domain;

import java.util.UUID;

public record ListingReviewSummary(
    UUID listingId,
    Double averageRating,
    long reviewCount
) {}

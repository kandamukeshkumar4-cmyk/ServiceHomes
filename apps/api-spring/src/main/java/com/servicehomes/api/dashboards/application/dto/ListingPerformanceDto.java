package com.servicehomes.api.dashboards.application.dto;

import java.util.UUID;

public record ListingPerformanceDto(
    UUID listingId,
    String listingTitle,
    String coverUrl,
    long bookingCount,
    Double averageRating,
    long reviewCount
) {}

package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingSearchResult(
    UUID id,
    String title,
    String coverUrl,
    String city,
    String country,
    BigDecimal nightlyPrice,
    String categoryName,
    Double latitude,
    Double longitude,
    int maxGuests,
    int bedrooms,
    int beds,
    int bathrooms,
    Double distanceKm,
    BigDecimal averageRating,
    long reviewCount,
    BigDecimal trustScore,
    boolean isSaved
) {}

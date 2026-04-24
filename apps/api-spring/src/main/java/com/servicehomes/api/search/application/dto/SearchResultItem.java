package com.servicehomes.api.search.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SearchResultItem(
    UUID id,
    String title,
    String coverUrl,
    String city,
    String country,
    String state,
    BigDecimal nightlyPrice,
    String categoryName,
    Double latitude,
    Double longitude,
    Double distanceKm,
    Integer maxGuests,
    Integer bedrooms,
    Integer beds,
    Integer bathrooms,
    BigDecimal averageRating,
    Long reviewCount,
    BigDecimal trustScore,
    String propertyType,
    Boolean instantBook,
    List<String> amenityIds,
    boolean isSaved
) {
}

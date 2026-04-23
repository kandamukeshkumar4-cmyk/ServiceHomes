package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListingDto(
    UUID id,
    UUID hostId,
    String title,
    String description,
    CategoryDto category,
    String propertyType,
    int maxGuests,
    int bedrooms,
    int beds,
    int bathrooms,
    BigDecimal nightlyPrice,
    BigDecimal cleaningFee,
    BigDecimal serviceFee,
    String status,
    Instant createdAt,
    Instant updatedAt,
    Instant publishedAt,
    BigDecimal averageRating,
    long reviewCount,
    BigDecimal cleanlinessRating,
    BigDecimal accuracyRating,
    BigDecimal communicationRating,
    BigDecimal locationRating,
    BigDecimal valueRating,
    BigDecimal trustScore,
    LocationDto location,
    PolicyDto policy,
    List<PhotoDto> photos,
    List<AmenityDto> amenities
) {
    public record CategoryDto(UUID id, String name, String icon) {}
    public record LocationDto(String addressLine1, String addressLine2, String city, String state, String postalCode, String country, Double latitude, Double longitude) {}
    public record PolicyDto(String checkInTime, String checkOutTime, int minNights, Integer maxNights, String cancellationPolicy, boolean instantBook) {}
    public record PhotoDto(UUID id, String url, int orderIndex, boolean isCover) {}
    public record AmenityDto(UUID id, String name, String icon, String category) {}
}

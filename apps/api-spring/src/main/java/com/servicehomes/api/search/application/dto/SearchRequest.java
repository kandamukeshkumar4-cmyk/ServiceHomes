package com.servicehomes.api.search.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SearchRequest(
    String query,
    UUID categoryId,
    Integer guests,
    LocalDate checkIn,
    LocalDate checkOut,
    @Min(value = 0) BigDecimal minPrice,
    @Min(value = 0) BigDecimal maxPrice,
    List<UUID> amenityIds,
    Double lat,
    Double lng,
    @Positive @Max(100) Double radiusKm,
    Integer bedrooms,
    Integer beds,
    Integer bathrooms,
    List<String> propertyTypes,
    Boolean instantBook,
    SearchSort sort,
    Double swLat,
    Double swLng,
    Double neLat,
    Double neLng,
    @Min(0) int page,
    @Min(1) @Max(100) int size
) {
    public SearchRequest {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }

    public boolean hasGeoCoords() {
        return lat != null && lng != null;
    }

    public boolean hasBoundingBox() {
        return swLat != null && swLng != null && neLat != null && neLng != null;
    }

    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }
}

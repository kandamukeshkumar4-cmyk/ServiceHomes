package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SearchListingsRequest(
    String locationQuery,
    UUID categoryId,
    Integer guests,
    LocalDate checkIn,
    LocalDate checkOut,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    List<UUID> amenityIds,
    Double swLat,
    Double swLng,
    Double neLat,
    Double neLng
) {}

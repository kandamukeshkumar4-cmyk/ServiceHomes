package com.servicehomes.api.dashboards.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TripDto(
    UUID reservationId,
    UUID listingId,
    String listingTitle,
    String listingCoverUrl,
    String listingCity,
    String listingCountry,
    UUID hostId,
    String hostDisplayName,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    int totalNights,
    BigDecimal totalAmount,
    String status,
    boolean canReview
) {}

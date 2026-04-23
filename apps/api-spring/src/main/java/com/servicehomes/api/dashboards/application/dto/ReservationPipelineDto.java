package com.servicehomes.api.dashboards.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationPipelineDto(
    UUID reservationId,
    UUID listingId,
    String listingTitle,
    String listingCoverUrl,
    String listingCity,
    String listingCountry,
    UUID guestId,
    String guestDisplayName,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    int totalNights,
    BigDecimal totalAmount,
    String status
) {}

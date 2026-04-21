package com.servicehomes.api.reservations.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReservationDto(
    UUID id,
    UUID listingId,
    String listingTitle,
    String listingCoverUrl,
    String listingCity,
    String listingCountry,
    UUID guestId,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests,
    int totalNights,
    BigDecimal nightlyPrice,
    BigDecimal cleaningFee,
    BigDecimal serviceFee,
    BigDecimal totalAmount,
    String status,
    String hostDisplayName
) {}

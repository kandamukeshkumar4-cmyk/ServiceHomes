package com.servicehomes.api.reservations.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record QuoteRequest(
    UUID listingId,
    LocalDate checkIn,
    LocalDate checkOut,
    int guests
) {}

package com.servicehomes.api.reservations.application.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record CreateReservationRequest(
    @NotNull UUID listingId,
    @NotNull @FutureOrPresent LocalDate checkIn,
    @NotNull @FutureOrPresent LocalDate checkOut,
    @NotNull @Min(1) Integer guests
) {}

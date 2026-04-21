package com.servicehomes.api.listings.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateListingRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 5000) String description,
    @NotNull UUID categoryId,
    @NotNull String propertyType,
    @NotNull @Min(1) @Max(50) Integer maxGuests,
    @NotNull @Min(0) Integer bedrooms,
    @NotNull @Min(0) Integer beds,
    @NotNull @Min(0) Integer bathrooms,
    @NotNull @DecimalMin("0.00") BigDecimal nightlyPrice,
    @DecimalMin("0.00") BigDecimal cleaningFee,
    @DecimalMin("0.00") BigDecimal serviceFee,
    @NotNull LocationDto location,
    @NotNull PolicyDto policy,
    List<UUID> amenityIds
) {
    public record LocationDto(
        @NotBlank @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @NotBlank @Size(max = 128) String city,
        @Size(max = 128) String state,
        @Size(max = 32) String postalCode,
        @NotBlank @Size(max = 128) String country,
        Double latitude,
        Double longitude
    ) {}

    public record PolicyDto(
        LocalTime checkInTime,
        LocalTime checkOutTime,
        @NotNull @Min(1) Integer minNights,
        Integer maxNights,
        String cancellationPolicy,
        Boolean instantBook
    ) {}
}

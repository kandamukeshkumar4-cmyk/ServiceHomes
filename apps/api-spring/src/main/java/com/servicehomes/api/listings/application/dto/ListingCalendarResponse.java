package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ListingCalendarResponse(
    UUID listingId,
    LocalDate startDate,
    LocalDate endDate,
    List<CalendarDayDto> days
) {
    public record CalendarDayDto(
        LocalDate date,
        boolean blocked,
        int minNights,
        BigDecimal nightlyPrice,
        boolean minNightsOverride,
        boolean priceOverride
    ) {}
}

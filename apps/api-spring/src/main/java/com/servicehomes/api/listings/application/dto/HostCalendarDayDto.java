package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record HostCalendarDayDto(
    LocalDate date,
    boolean blocked,
    boolean turnover,
    int minNights,
    BigDecimal baseNightlyPrice,
    BigDecimal seasonalMultiplier,
    BigDecimal weekendMultiplier,
    BigDecimal priceOverride,
    BigDecimal finalNightlyPrice,
    boolean hasPriceOverride,
    boolean hasSeasonalTemplate,
    boolean hasWeekendMultiplier
) {}

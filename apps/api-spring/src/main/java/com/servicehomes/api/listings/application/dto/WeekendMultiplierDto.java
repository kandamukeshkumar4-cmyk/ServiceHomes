package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WeekendMultiplierDto(
    UUID id,
    BigDecimal fridayMultiplier,
    BigDecimal saturdayMultiplier,
    BigDecimal sundayMultiplier
) {}

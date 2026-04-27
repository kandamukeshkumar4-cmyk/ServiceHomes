package com.servicehomes.api.listings.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateWeekendMultiplierRequest(
    @NotNull BigDecimal fridayMultiplier,
    @NotNull BigDecimal saturdayMultiplier,
    @NotNull BigDecimal sundayMultiplier
) {}

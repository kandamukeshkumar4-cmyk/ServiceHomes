package com.servicehomes.api.listings.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateSeasonalPricingTemplateRequest(
    @NotBlank String name,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull BigDecimal multiplier
) {}

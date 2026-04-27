package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SeasonalPricingTemplateDto(
    UUID id,
    String name,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal multiplier
) {}

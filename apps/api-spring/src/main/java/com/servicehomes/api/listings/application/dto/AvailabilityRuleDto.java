package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AvailabilityRuleDto(
    UUID id,
    String ruleType,
    LocalDate startDate,
    LocalDate endDate,
    BigDecimal value
) {}

package com.servicehomes.api.listings.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateListingAvailabilityRequest(
    @NotNull List<@Valid AvailabilityRuleInput> rules
) {
    public record AvailabilityRuleInput(
        @NotEmpty String ruleType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        BigDecimal value
    ) {}
}

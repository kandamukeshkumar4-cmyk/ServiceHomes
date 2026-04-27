package com.servicehomes.api.listings.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateLengthOfStayDiscountRequest(
    @NotNull int minNights,
    @NotNull BigDecimal discountPercent
) {}

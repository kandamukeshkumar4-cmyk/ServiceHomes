package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LengthOfStayDiscountDto(
    UUID id,
    int minNights,
    BigDecimal discountPercent
) {}

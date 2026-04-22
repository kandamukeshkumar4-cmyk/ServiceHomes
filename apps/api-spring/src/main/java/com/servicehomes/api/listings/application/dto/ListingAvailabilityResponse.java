package com.servicehomes.api.listings.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ListingAvailabilityResponse(
    UUID listingId,
    BigDecimal baseNightlyPrice,
    int defaultMinNights,
    List<AvailabilityRuleDto> rules
) {}

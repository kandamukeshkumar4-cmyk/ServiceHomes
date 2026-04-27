package com.servicehomes.api.wishlists.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ListingSummaryDto(UUID id, String title, String thumbnail, BigDecimal price, BigDecimal rating) {}

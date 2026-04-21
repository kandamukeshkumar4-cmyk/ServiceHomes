package com.servicehomes.api.reservations.application.dto;

import java.math.BigDecimal;

public record QuoteResponse(
    int totalNights,
    BigDecimal nightlyPrice,
    BigDecimal subtotal,
    BigDecimal cleaningFee,
    BigDecimal serviceFee,
    BigDecimal totalAmount
) {}

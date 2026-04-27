package com.servicehomes.api.listings.application.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateTurnoverDayRequest(
    @NotNull int bufferDays
) {}

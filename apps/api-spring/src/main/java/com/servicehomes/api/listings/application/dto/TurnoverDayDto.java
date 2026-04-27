package com.servicehomes.api.listings.application.dto;

import java.util.UUID;

public record TurnoverDayDto(
    UUID id,
    int bufferDays
) {}

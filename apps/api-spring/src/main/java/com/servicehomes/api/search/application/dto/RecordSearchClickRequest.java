package com.servicehomes.api.search.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecordSearchClickRequest(
    @NotNull UUID searchQueryId,
    @NotNull UUID listingId,
    @NotNull Integer resultPosition,
    String deviceType
) {
}

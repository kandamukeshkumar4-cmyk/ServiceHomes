package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecordListingViewRequest(@NotNull UUID listingId, String sourcePage) {}

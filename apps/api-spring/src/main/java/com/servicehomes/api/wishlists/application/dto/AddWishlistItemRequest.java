package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddWishlistItemRequest(@NotNull UUID listingId, @Size(max = 2000) String note, String sourcePage) {}

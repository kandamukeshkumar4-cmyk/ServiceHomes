package com.servicehomes.api.wishlists.application.dto;

import java.time.Instant;
import java.util.UUID;

public record WishlistItemDto(UUID id, ListingSummaryDto listing, String note, int sortOrder, Instant addedAt) {}

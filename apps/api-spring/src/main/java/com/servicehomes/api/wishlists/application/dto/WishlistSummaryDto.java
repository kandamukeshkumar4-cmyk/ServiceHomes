package com.servicehomes.api.wishlists.application.dto;

import java.time.Instant;
import java.util.UUID;

public record WishlistSummaryDto(UUID id, UUID ownerId, String title, String description, String coverPhotoUrl, boolean isPublic, boolean owner, int collaboratorCount, long itemCount, Instant updatedAt) {}

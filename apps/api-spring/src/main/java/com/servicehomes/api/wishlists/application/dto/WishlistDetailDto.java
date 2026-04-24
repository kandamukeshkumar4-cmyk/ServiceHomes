package com.servicehomes.api.wishlists.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WishlistDetailDto(UUID id, UUID ownerId, String title, String description, String coverPhotoUrl, boolean isPublic, boolean owner, int collaboratorCount, long itemCount, Instant updatedAt, String shareToken, List<UUID> collaboratorIds, List<WishlistItemDto> items, long totalItems, boolean editable) {}

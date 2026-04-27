package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderWishlistItemsRequest(@NotEmpty List<UUID> itemIds) {}

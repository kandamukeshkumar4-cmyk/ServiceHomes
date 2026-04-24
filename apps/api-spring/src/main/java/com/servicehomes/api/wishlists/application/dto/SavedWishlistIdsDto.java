package com.servicehomes.api.wishlists.application.dto;

import java.util.List;
import java.util.UUID;

public record SavedWishlistIdsDto(List<UUID> wishlistIds) {}

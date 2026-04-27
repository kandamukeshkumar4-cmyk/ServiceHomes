package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateWishlistItemRequest(@Size(max = 2000) String note) {}

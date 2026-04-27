package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWishlistRequest(
    @NotBlank @Size(max = 160) String title,
    @Size(max = 2000) String description,
    boolean isPublic
) {}

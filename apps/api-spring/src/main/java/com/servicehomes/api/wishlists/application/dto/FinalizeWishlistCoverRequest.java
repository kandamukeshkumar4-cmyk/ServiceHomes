package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.NotBlank;

public record FinalizeWishlistCoverRequest(@NotBlank String s3Key) {}

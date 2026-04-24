package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record WishlistCoverUploadRequest(@NotBlank String contentType, @Positive @Max(2_097_152) long contentLength) {}

package com.servicehomes.api.reviews.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerateReviewRequest(
    @NotBlank @Size(max = 32) String moderationStatus,
    @Size(max = 2000) String notes
) {}

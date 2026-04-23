package com.servicehomes.api.reviews.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportReviewRequest(
    @NotBlank @Size(max = 64) String reason,
    @Size(max = 2000) String details
) {}

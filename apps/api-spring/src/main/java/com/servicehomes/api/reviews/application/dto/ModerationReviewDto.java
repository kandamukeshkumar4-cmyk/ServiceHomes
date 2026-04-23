package com.servicehomes.api.reviews.application.dto;

import java.util.List;

public record ModerationReviewDto(
    ReviewDto review,
    List<ReviewReportDto> reports
) {}

package com.servicehomes.api.reviews.application.dto;

import java.util.List;

public record ListingReviewsResponse(
    double averageRating,
    long reviewCount,
    List<ReviewDto> content,
    long totalElements,
    int totalPages,
    int number,
    int size,
    boolean first,
    boolean last
) {}

package com.servicehomes.api.reviews.application.dto;

import java.util.List;

public record ListingReviewsResponse(
    double averageRating,
    long reviewCount,
    Double cleanlinessRating,
    Double accuracyRating,
    Double communicationRating,
    Double locationRating,
    Double valueRating,
    double trustScore,
    List<ReviewDto> content,
    long totalElements,
    int totalPages,
    int number,
    int size,
    boolean first,
    boolean last
) {}

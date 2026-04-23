package com.servicehomes.api.reviews.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewReportDto(
    UUID id,
    UUID reviewId,
    UUID reporterId,
    String reason,
    String details,
    String status,
    Instant createdAt,
    Instant resolvedAt,
    UUID resolvedBy
) {}

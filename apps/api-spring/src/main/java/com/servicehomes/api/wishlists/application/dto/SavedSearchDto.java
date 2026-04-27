package com.servicehomes.api.wishlists.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SavedSearchDto(UUID id, String name, Map<String, Object> filters, String locationQuery, Double geoCenterLat, Double geoCenterLng, Double radiusKm, boolean notifyNewResults, Integer resultCountSnapshot, Instant createdAt) {}

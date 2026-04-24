package com.servicehomes.api.wishlists.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record SaveSearchRequest(@NotBlank @Size(max = 160) String name, @NotNull Map<String, Object> filters, @Size(max = 240) String locationQuery, Double geoCenterLat, Double geoCenterLng, Double radiusKm, boolean notifyNewResults) {}

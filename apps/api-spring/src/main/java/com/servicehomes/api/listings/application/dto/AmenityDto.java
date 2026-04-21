package com.servicehomes.api.listings.application.dto;

import java.util.UUID;

public record AmenityDto(
    UUID id,
    String name,
    String icon,
    String category
) {}

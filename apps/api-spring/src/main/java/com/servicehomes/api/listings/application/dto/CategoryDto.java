package com.servicehomes.api.listings.application.dto;

import java.util.UUID;

public record CategoryDto(
    UUID id,
    String name,
    String icon,
    String description
) {}

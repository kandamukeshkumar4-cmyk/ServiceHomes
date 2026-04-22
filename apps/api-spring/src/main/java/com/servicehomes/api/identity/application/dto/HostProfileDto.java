package com.servicehomes.api.identity.application.dto;

import com.servicehomes.api.listings.application.dto.ListingCardDto;

import java.time.Instant;
import java.util.List;

public record HostProfileDto(
    String hostId,
    String displayName,
    String bio,
    String avatarUrl,
    String location,
    List<String> languages,
    Instant memberSince,
    Integer responseRate,
    long listingsCount,
    List<ListingCardDto> listings
) {}

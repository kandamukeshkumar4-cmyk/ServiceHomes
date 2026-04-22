package com.servicehomes.api.identity.application.dto;

import java.time.Instant;
import java.util.List;

public record ProfileDto(
    String firstName,
    String lastName,
    String displayName,
    String bio,
    String avatarUrl,
    String phoneNumber,
    String location,
    List<String> languages,
    Instant createdAt
) {}

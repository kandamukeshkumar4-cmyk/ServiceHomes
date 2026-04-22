package com.servicehomes.api.identity.application.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateProfileRequest(
    @Size(max = 150) String displayName,
    @Size(max = 500) String bio,
    @Size(max = 500) String avatarUrl,
    @Size(max = 32) String phoneNumber,
    @Size(max = 255) String location,
    List<@Size(max = 64) String> languages
) {}

package com.servicehomes.api.identity.application.dto;

import java.util.List;

public record MeDto(
    String id,
    String email,
    boolean emailVerified,
    List<String> roles,
    ProfileDto profile
) {}

package com.servicehomes.api.identity.application.dto;

public record AvatarUploadResponse(
    String uploadUrl,
    String publicUrl
) {}

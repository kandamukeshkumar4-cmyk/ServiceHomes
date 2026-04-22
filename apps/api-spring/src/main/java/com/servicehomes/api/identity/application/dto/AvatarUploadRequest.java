package com.servicehomes.api.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AvatarUploadRequest(
    @NotBlank String fileName,
    @NotBlank String contentType
) {}
